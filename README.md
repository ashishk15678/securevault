# SecureVault - Secure File Storage System

## Overview

SecureVault is a comprehensive secure file storage application built with Java Swing, featuring end-to-end encryption, user authentication, and a modern Clerk.com-inspired user interface. The system provides secure file upload, storage, and download capabilities with AES-256 encryption.

## Features

### Authentication & Security

- **User Registration & Login**: Secure user authentication with email and password
- **RSA-based Password Hashing**: Passwords are combined with email, hashed with SHA-256, and signed with RSA private key
- **Token-based Session Management**: JWT-like tokens for secure session handling
- **Role-based Access Control**: Support for ADMIN and CLIENT roles

### File Management

- **Secure File Upload**: Files are encrypted with AES-256 before storage
- **Unique Encryption Keys**: Each file gets its own randomly generated AES-256 key
- **File Download & Decryption**: Secure download with key-based decryption
- **Key Management**: View, copy, and verify AES keys for uploaded files
- **File Verification**: Verify file integrity and encryption keys

### User Interface

- **Modern Dashboard**: Clean, Clerk.com-inspired design
- **Real-time Statistics**: File count, storage usage, and activity tracking
- **File Management Table**: View all uploaded files with metadata
- **Settings Management**: Edit profile, change password, view account information

### Technical Architecture

- **Custom Database**: Lightweight database implementation with table, index, and transaction support
- **Custom Filesystem**: File storage system with metadata management
- **AES Encryption**: Custom AES-256 implementation for file encryption
- **RSA Encryption**: RSA-2048 for password signing and token generation

## Project Structure

```
src/
├── auth/
│   └── AuthService.java          # Authentication and authorization
├── crypto/
│   ├── AESEncryption.java        # AES-256 encryption implementation
│   └── RSAEncryption.java        # RSA encryption for passwords and tokens
├── db/
│   └── Database.java             # Custom database implementation
├── storage/
│   └── Filesystem.java           # File storage and metadata management
├── ui/
│   ├── Client.java               # Login/Registration UI
│   ├── ClientService.java        # Main dashboard window
│   ├── Dashboard.java            # Dashboard component
│   ├── FilesPage.java            # File management page
│   ├── Settings.java             # Settings page
│   └── StatCard.java             # Statistics card component
└── Main.java                     # Application entry point
```

## Getting Started

### Prerequisites

- Java JDK 8 or higher
- Any Java IDE (IntelliJ IDEA, Eclipse, etc.)

### Running the Application

1. Clone or download the project
2. Open the project in your IDE
3. Compile all Java files
4. Run `Main.java`
5. Register a new account or login with default admin:
   - Email: `admin@securevault.com`
   - Password: `adminadmin`

## Usage

### Registration

1. Click "Don't have an account? Sign up"
2. Enter your full name, email, and password
3. Confirm your password
4. Click "Create Account"

### File Upload

1. Navigate to "My Files" page
2. Click "Upload File" button
3. Select a file from your computer
4. The file will be encrypted with AES-256
5. **Save the displayed AES key** - you'll need it to download the file later

### File Download

1. Go to "My Files" page
2. Find your file in the table
3. Click "Download" button
4. Enter the AES key (pre-filled with stored key)
5. Choose save location
6. File will be decrypted and saved

### View Encryption Key

1. Click "Show Key" button next to any file
2. Copy the AES key to clipboard
3. Save it securely for future use

### Verify File

1. Click "Verify" button next to any file
2. Enter the AES key
3. System will verify the key can decrypt the file
4. Shows success or failure message

## Security Features

- **AES-256 Encryption**: Industry-standard encryption for file storage
- **RSA Password Signing**: Secure password storage using RSA signatures
- **Unique Keys**: Each file has its own encryption key
- **Key Verification**: Verify encryption keys before download
- **Secure Storage**: Encrypted files stored separately from keys

## Future Work

### Enhanced Security

- [ ] Implement key derivation functions (PBKDF2, Argon2) for password hashing
- [ ] Add two-factor authentication (2FA) support
- [ ] Implement secure key escrow/recovery mechanism
- [ ] Add file integrity verification using HMAC
- [ ] Support for encrypted key storage using master password

### User Experience

- [ ] Add drag-and-drop file upload
- [ ] Implement file preview functionality
- [ ] Add file sharing capabilities with encrypted links
- [ ] Create mobile application version
- [ ] Add file versioning and history

### Advanced Features

- [ ] Cloud storage integration (AWS S3, Google Cloud Storage)
- [ ] File compression before encryption
- [ ] Batch file operations (upload/download multiple files)
- [ ] File search and filtering capabilities
- [ ] Activity logs and audit trails

### Performance & Scalability

- [ ] Implement database connection pooling
- [ ] Add caching layer for frequently accessed files
- [ ] Support for large file streaming
- [ ] Database optimization and indexing improvements
- [ ] Multi-threading for file operations

### Integration & API

- [ ] RESTful API for programmatic access
- [ ] Web-based interface using Spring Boot
- [ ] CLI (Command Line Interface) version
- [ ] Integration with external authentication providers (OAuth, SAML)
- [ ] Webhook support for file events

### Monitoring & Analytics

- [ ] User activity dashboard
- [ ] Storage usage analytics
- [ ] Security event logging
- [ ] Performance metrics and monitoring
- [ ] Automated backup and recovery

### Compliance & Standards

- [ ] GDPR compliance features
- [ ] Data retention policies
- [ ] Compliance reporting
- [ ] Encryption standards compliance (FIPS 140-2)
- [ ] Security audit capabilities

## Technical Details

### Encryption

- **AES-256**: Advanced Encryption Standard with 256-bit keys
- **RSA-2048**: RSA encryption with 2048-bit keys for password signing
- **SHA-256**: Secure Hash Algorithm for password hashing

### Database

- Custom implementation with support for:
  - Tables, columns, and data types
  - Indexes for performance
  - Transactions with commit/rollback
  - Persistent storage

### Filesystem

- Custom file storage with:
  - Metadata management
  - Directory structure
  - File search capabilities
  - Storage statistics

## License

This project is developed for educational purposes.

## Contributors

- Development Team

## Acknowledgments

- UI design inspired by Clerk.com
- Custom encryption implementations for educational purposes


