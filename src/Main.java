import ui.Client;
import auth.AuthService;
import db.Database;
import crypto.RSAEncryption;

public class Main {
    public static void main(String[] args) {
        // Initialize database
        Database database = new Database("./data");
        
        // Initialize RSA encryption
        RSAEncryption rsa = new RSAEncryption();
        
        // Initialize auth service
        AuthService authService = new AuthService(database, rsa, 24L * 60 * 60 * 1000);
        
        // Start the client UI
        Client client = new Client(authService);
        client.init();
    }
}
