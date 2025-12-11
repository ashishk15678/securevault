package auth;

import crypto.RSAEncryption;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import db.*;

public class AuthService {

    public enum Role { ADMIN, CLIENT }

    private final Database database;
    private final RSAEncryption rsa;

    // in-memory token blacklist/active sessions (simple)
    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();

    // token lifetime (ms)
    private final long tokenTtlMs;

    public AuthService(Database database, RSAEncryption rsa, long tokenTtlMs) {
        this.database = database;
        this.rsa = rsa;
        this.tokenTtlMs = tokenTtlMs <= 0 ? 24L * 60 * 60 * 1000 : tokenTtlMs;
        initializeUserTable();
        ensureDefaultAdmin();
    }

    public boolean createUser(String name, String email, String password, Role role) {
        try {
            if (name == null || name.isEmpty() || email == null || email.isEmpty() || password == null || password.isEmpty()) return false;
            
            // Check if email already exists
            List<Database.Record> existing = database.select("users", "email = " + email);
            if (!existing.isEmpty()) {
                return false; // Email already registered
            }
            
            // Combine email and password, then hash with SHA-256, then sign with RSA private key
            String combined = email + password;
            byte[] hashBytes = sha256(combined.getBytes(StandardCharsets.UTF_8));
            byte[] signedHash = signHash(hashBytes, rsa.getKeyPair().getPrivateKey());
            String passwordHash = Base64.getEncoder().encodeToString(signedHash);
            
            Map<String, Object> record = new HashMap<>();
            record.put("name", name);
            record.put("email", email);
            record.put("password_hash", passwordHash);
            record.put("role", role.name());
            record.put("created_at", System.currentTimeMillis());
            return database.insert("users", record);
        } catch (Exception e) {
            System.err.println("AuthService.createUser error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String email) {
        try {
            int n = database.delete("users", "email = " + email);
            return n > 0;
        } catch (Exception e) {
            System.err.println("AuthService.deleteUser error: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, String>> listUsers() {
        List<Map<String, String>> users = new ArrayList<>();
        try {
            for (Database.Record rec : database.selectAll("users")) {
                Map<String, String> row = new HashMap<>();
                row.put("name", String.valueOf(rec.getValue("name")));
                row.put("email", String.valueOf(rec.getValue("email")));
                row.put("role", String.valueOf(rec.getValue("role")));
                row.put("created_at", String.valueOf(rec.getValue("created_at")));
                users.add(row);
            }
        } catch (Exception e) {
            System.err.println("AuthService.listUsers error: " + e.getMessage());
        }
        return users;
    }

    public String login(String email, String password) {
        try {
            List<Database.Record> recs = database.select("users", "email = " + email);
            if (recs.isEmpty()) return null;
            Database.Record rec = recs.get(0);
            
            // Verify password: combine email+password, hash, sign with RSA, and compare
            String combined = email + password;
            byte[] hashBytes = sha256(combined.getBytes(StandardCharsets.UTF_8));
            byte[] signedHash = signHash(hashBytes, rsa.getKeyPair().getPrivateKey());
            String passwordHash = Base64.getEncoder().encodeToString(signedHash);
            
            String storedHash = String.valueOf(rec.getValue("password_hash"));
            if (!passwordHash.equals(storedHash)) return null;
            
            String name = String.valueOf(rec.getValue("name"));
            String role = String.valueOf(rec.getValue("role"));
            long exp = System.currentTimeMillis() + tokenTtlMs;
            String payload = "sub=" + email + "&name=" + name + "&role=" + role + "&exp=" + exp;
            String token = signToken(payload);
            activeTokens.add(token);
            return token;
        } catch (Exception e) {
            System.err.println("AuthService.login error: " + e.getMessage());
            return null;
        }
    }
    
    public Map<String, String> getUserByEmail(String email) {
        try {
            List<Database.Record> recs = database.select("users", "email = " + email);
            if (recs.isEmpty()) return null;
            Database.Record rec = recs.get(0);
            Map<String, String> user = new HashMap<>();
            user.put("name", String.valueOf(rec.getValue("name")));
            user.put("email", String.valueOf(rec.getValue("email")));
            user.put("role", String.valueOf(rec.getValue("role")));
            user.put("created_at", String.valueOf(rec.getValue("created_at")));
            return user;
        } catch (Exception e) {
            System.err.println("AuthService.getUserByEmail error: " + e.getMessage());
            return null;
        }
    }

    public void logout(String token) {
        if (token != null) activeTokens.remove(token);
    }

    public AuthResult verify(String bearerHeader) {
        try {
            if (bearerHeader == null || !bearerHeader.startsWith("Bearer ")) {
                return AuthResult.unauthorized("Missing Bearer token");
            }
            String token = bearerHeader.substring("Bearer ".length()).trim();
            if (!activeTokens.contains(token)) {
                // optionally still allow stateless validation without activeTokens
                // but we keep it simple
            }
            String[] parts = token.split("\\.");
            if (parts.length != 2) return AuthResult.unauthorized("Malformed token");
            byte[] payloadBytes = base64UrlDecode(parts[0]);
            byte[] sigBytes = base64UrlDecode(parts[1]);

            // verify signature
            byte[] expectedHash = sha256(payloadBytes);
            byte[] verified = verifySignature(sigBytes, rsa.getKeyPair().getPublicKey());
            if (!Arrays.equals(expectedHash, verified)) {
                return AuthResult.unauthorized("Invalid signature");
            }

            // parse payload
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            Map<String, String> map = parseKv(payload);
            String sub = map.get("sub");
            String role = map.get("role");
            long exp = Long.parseLong(map.get("exp"));
            if (System.currentTimeMillis() > exp) {
                return AuthResult.unauthorized("Token expired");
            }
            return AuthResult.ok(sub, role, token);
        } catch (Exception e) {
            return AuthResult.unauthorized("Error: " + e.getMessage());
        }
    }

    private String signToken(String payload) {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] hash = sha256(payloadBytes);
        byte[] signature = signHash(hash, rsa.getKeyPair().getPrivateKey());
        return base64UrlEncode(payloadBytes) + "." + base64UrlEncode(signature);
    }

    private byte[] signHash(byte[] hash, RSAEncryption.RSAPrivateKey privateKey) {
        // Basic RSA "sign": hash^d mod n
        BigInteger m = new BigInteger(1, hash);
        BigInteger s = m.modPow(privateKey.getExponent(), privateKey.getModulus());
        return s.toByteArray();
    }

    private byte[] verifySignature(byte[] signature, RSAEncryption.RSAPublicKey publicKey) {
        // verify: s^e mod n -> should equal hash
        BigInteger s = new BigInteger(1, signature);
        BigInteger m = s.modPow(publicKey.getExponent(), publicKey.getModulus());
        byte[] out = m.toByteArray();
        // Normalize to 32 bytes (SHA-256)
        if (out.length == 32) return out;
        if (out.length > 32) {
            return Arrays.copyOfRange(out, out.length - 32, out.length);
        } else {
            byte[] padded = new byte[32];
            System.arraycopy(out, 0, padded, 32 - out.length, out.length);
            return padded;
        }
    }

    private void initializeUserTable() {
        try {
            // Check if table exists with old schema, if so, drop it
            List<String> tables = database.listTables();
            if (tables.contains("users")) {
                // Try to check if it has the old schema by attempting to select
                try {
                    database.select("users", "email = test");
                } catch (Exception e) {
                    // Old schema, drop and recreate
                    database.dropTable("users");
                }
            }
            
            List<Database.Column> cols = Arrays.asList(
                    new Database.Column("name", Database.DataType.STRING, false, false),
                    new Database.Column("email", Database.DataType.STRING, false, true),
                    new Database.Column("password_hash", Database.DataType.STRING, false, false),
                    new Database.Column("role", Database.DataType.STRING, false, false),
                    new Database.Column("created_at", Database.DataType.TIMESTAMP, false, false)
            );
            database.createTable("users", cols);
            database.createIndex("users", "email");
        } catch (Exception e) {
            System.err.println("AuthService.initializeUserTable error: " + e.getMessage());
        }
    }

    private void ensureDefaultAdmin() {
        try {
            List<Database.Record> recs = database.select("users", "email = admin@securevault.com");
            if (recs.isEmpty()) {
                // default password: adminadmin (please change via UI)
                createUser("Admin User", "admin@securevault.com", "adminadmin", Role.ADMIN);
                System.out.println("Default admin created: email=admin@securevault.com, password=adminadmin");
            }
        } catch (Exception e) {
            System.err.println("AuthService.ensureDefaultAdmin error: " + e.getMessage());
        }
    }

    private static String sha256Hex(String s) {
        return bytesToHex(sha256(s.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private static Map<String, String> parseKv(String payload) {
        Map<String, String> map = new HashMap<>();
        String[] parts = payload.split("&");
        for (String p : parts) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    public static class AuthResult {
        public final boolean ok;
        public final String username;
        public final String role;
        public final String token;
        public final String error;

        private AuthResult(boolean ok, String username, String role, String token, String error) {
            this.ok = ok;
            this.username = username;
            this.role = role;
            this.token = token;
            this.error = error;
        }

        public static AuthResult ok(String username, String role, String token) {
            return new AuthResult(true, username, role, token, null);
        }

        public static AuthResult unauthorized(String error) {
            return new AuthResult(false, null, null, null, error);
        }

        public boolean isAdmin() {
            return ok && "ADMIN".equalsIgnoreCase(role);
        }
    }
}



