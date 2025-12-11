package crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * RSA Encryption implementation using java.math.BigInteger for reliable arithmetic.
 * Supports key generation, encryption, and decryption based on the principles
 * of modular exponentiation by repeated squaring.
 */
public class RSAEncryption {

    // The standard key size for modern RSA, as 2048 bits is currently considered secure.
    private static final int DEFAULT_KEY_SIZE = 2048;
    private RSAKeyPair keyPair;

    // Use SecureRandom for cryptographic operations like key generation
    private static final Random RANDOM = new SecureRandom();

    public RSAEncryption() {
        this.keyPair = generateKeyPair(DEFAULT_KEY_SIZE);
    }

    public RSAEncryption(int keySize) {
        this.keyPair = generateKeyPair(keySize);
    }

    public RSAEncryption(RSAKeyPair keyPair) {
        this.keyPair = keyPair;
    }

    /**
     * Generate a robust RSA key pair.
     * The core mathematical steps (modPow and modInverse) are handled by BigInteger,
     * which uses efficient algorithms like the one described as "exponentiation by
     * repeated squaring and multiplication" (modPow) and the Extended Euclidean
     * Algorithm (modInverse).
     */
    public static RSAKeyPair generateKeyPair(int keySize) {
        // Step 1: Generate two large prime numbers (p and q).
        // 50% bit length for each prime ensures the product n is the desired key size.
        BigInteger p = BigInteger.probablePrime(keySize / 2, RANDOM);
        BigInteger q = BigInteger.probablePrime(keySize / 2, RANDOM);

        // Ensure p != q (highly unlikely with large primes, but good practice)
        while (p.equals(q)) {
            q = BigInteger.probablePrime(keySize / 2, RANDOM);
        }

        // Step 2: Calculate the modulus n = p * q.
        BigInteger n = p.multiply(q);

        // Step 3: Calculate Euler's totient function φ(n) = (p-1)(q-1).
        BigInteger pMinus1 = p.subtract(BigInteger.ONE);
        BigInteger qMinus1 = q.subtract(BigInteger.ONE);
        BigInteger phi = pMinus1.multiply(qMinus1);

        // Step 4: Choose the public exponent e. Commonly 65537, which is a prime number.
        BigInteger e = new BigInteger("65537");

        // Ensure gcd(e, φ(n)) = 1. BigInteger.gcd handles the 'g' method from the original code.
        // If 65537 is not coprime, find the next one, though this is rare.
        while (!phi.gcd(e).equals(BigInteger.ONE)) {
            e = e.add(BigInteger.TWO); // Test only odd numbers as phi is even
        }

        // Step 5: Calculate the private exponent d such that e * d ≡ 1 (mod φ(n)).
        // BigInteger.modInverse implements the Extended Euclidean Algorithm.
        BigInteger d = e.modInverse(phi);

        return new RSAKeyPair(
                new RSAPublicKey(n, e),
                new RSAPrivateKey(n, d)
        );
    }

    /**
     * Encrypt data using the public key.
     * C ≡ M^e (mod n). This uses the 'exponentiation by repeated squaring' procedure.
     */
    public byte[] encrypt(byte[] data, RSAPublicKey publicKey) {
        // Convert byte array to a positive BigInteger message M
        BigInteger message = new BigInteger(1, data);

        // Check if message is within the bounds [0, n-1]
        if (message.compareTo(publicKey.getModulus()) >= 0) {
            throw new IllegalArgumentException("Message is too large for the modulus (n).");
        }

        // C = M^e mod n. The BigInteger.modPow method is the optimized implementation.
        BigInteger encrypted = message.modPow(publicKey.getExponent(), publicKey.getModulus());
        return encrypted.toByteArray();
    }

    /**
     * Decrypt data using the private key.
     * M ≡ C^d (mod n). This also uses the 'exponentiation by repeated squaring' procedure.
     */
    public byte[] decrypt(byte[] encryptedData, RSAPrivateKey privateKey) {
        // Convert byte array to a positive BigInteger ciphertext C
        BigInteger encrypted = new BigInteger(1, encryptedData);

        // M = C^d mod n. The BigInteger.modPow method is the optimized implementation.
        BigInteger decrypted = encrypted.modPow(privateKey.getExponent(), privateKey.getModulus());

        // Return the decrypted message bytes
        return decrypted.toByteArray();
    }

    // Simple getters and setters

    public RSAKeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(RSAKeyPair keyPair) {
        this.keyPair = keyPair;
    }

    // Key Classes (using java.math.BigInteger)

    /**
     * RSA Public Key: (n, e)
     */
    public static class RSAPublicKey {
        private final BigInteger modulus; // n
        private final BigInteger exponent; // e (public exponent)

        public RSAPublicKey(BigInteger modulus, BigInteger exponent) {
            this.modulus = modulus;
            this.exponent = exponent;
        }

        public BigInteger getModulus() {
            return modulus;
        }

        public BigInteger getExponent() {
            return exponent;
        }

        // Utility method to serialize key for storage/transmission
        public byte[] toBytes() {
            // A simple string representation for demonstration
            return (modulus.toString(16) + ":" + exponent.toString(16)).getBytes();
        }

        public static RSAPublicKey fromBytes(byte[] data) {
            String[] parts = new String(data).split(":");
            return new RSAPublicKey(new BigInteger(parts[0], 16), new BigInteger(parts[1], 16));
        }
    }

    /**
     * RSA Private Key: (n, d)
     */
    public static class RSAPrivateKey {
        private final BigInteger modulus; // n
        private final BigInteger exponent; // d (private exponent)

        public RSAPrivateKey(BigInteger modulus, BigInteger exponent) {
            this.modulus = modulus;
            this.exponent = exponent;
        }

        public BigInteger getModulus() {
            return modulus;
        }

        public BigInteger getExponent() {
            return exponent;
        }

        // Utility method to serialize key for storage/transmission
        public byte[] toBytes() {
            // A simple string representation for demonstration
            return (modulus.toString(16) + ":" + exponent.toString(16)).getBytes();
        }

        public static RSAPrivateKey fromBytes(byte[] data) {
            String[] parts = new String(data).split(":");
            return new RSAPrivateKey(new BigInteger(parts[0], 16), new BigInteger(parts[1], 16));
        }
    }

    /**
     * RSA Key Pair container
     */
    public static class RSAKeyPair {
        private final RSAPublicKey publicKey;
        private final RSAPrivateKey privateKey;

        public RSAKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public RSAPublicKey getPublicKey() {
            return publicKey;
        }

        public RSAPrivateKey getPrivateKey() {
            return privateKey;
        }
    }
}

/*
 * Note on the provided custom BigInteger utility methods:
 * The original code contained simplified and incorrect methods for arithmetic
 * (addArrays, subtractArrays, etc.), primality testing (isPrime), and the
 * modInverse and modPow methods were re-implemented outside the custom BigInteger.
 * * In this refactored version:
 * 1. The custom BigInteger class is REMOVED.
 * 2. All cryptographic-grade arithmetic (modPow, gcd, modInverse, primality)
 * is delegated to the robust, efficient, and cryptographically sound
 * java.math.BigInteger class.
 * 3. The separate 'modPow' and 'modInverse' helper methods from the original
 * code are no longer necessary as their functionality is directly provided
 * by BigInteger.modPow and BigInteger.modInverse.
 * 4. Key Generation is simplified using BigInteger.probablePrime and BigInteger.modInverse,
 * eliminating the error-prone custom prime generation and modular inverse logic.
 */

