package crypto;

import java.util.Arrays;
import java.util.Random;

/**
 * AES Encryption implementation from scratch
 * Supports AES-128, AES-192, and AES-256
 */
public class AESEncryption {

    // AES constants
    private static final int BLOCK_SIZE = 16; // 128 bits
    private static final int[] S_BOX = {
            0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
            0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
            0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
            0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
            0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
            0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
            0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
            0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
            0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
            0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
            0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
            0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
            0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
            0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
            0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
            0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    };

    private static final int[] INVERSE_S_BOX = {
            0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
            0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
            0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
            0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
            0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
            0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
            0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
            0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
            0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
            0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
            0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
            0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
            0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
            0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
            0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
            0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
    };

    private static final int[] RCON = {
            0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
    };

    private int keySize;
    private int numRounds;

    public AESEncryption(int keySize) {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new IllegalArgumentException("Key size must be 128, 192, or 256 bits");
        }
        this.keySize = keySize;
        this.numRounds = keySize == 128 ? 10 : keySize == 192 ? 12 : 14;
    }

    /**
     * Generate a random AES key
     */
    public byte[] generateKey() {
        Random random = new Random();
        byte[] key = new byte[keySize / 8];
        random.nextBytes(key);
        return key;
    }

    /**
     * Encrypt data using AES
     */
    public byte[] encrypt(byte[] data, byte[] key) {
        if (key.length != keySize / 8) {
            throw new IllegalArgumentException("Invalid key length");
        }

        // Pad data to block size
        byte[] paddedData = padData(data);

        // Generate round keys
        byte[][] roundKeys = generateRoundKeys(key);

        // Encrypt each block
        byte[] encrypted = new byte[paddedData.length];
        for (int i = 0; i < paddedData.length; i += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(paddedData, i, i + BLOCK_SIZE);
            byte[] encryptedBlock = encryptBlock(block, roundKeys);
            System.arraycopy(encryptedBlock, 0, encrypted, i, BLOCK_SIZE);
        }

        return encrypted;
    }

    /**
     * Decrypt data using AES
     */
    public byte[] decrypt(byte[] encryptedData, byte[] key) {
        if (key.length != keySize / 8) {
            throw new IllegalArgumentException("Invalid key length");
        }

        // Generate round keys
        byte[][] roundKeys = generateRoundKeys(key);

        // Decrypt each block
        byte[] decrypted = new byte[encryptedData.length];
        for (int i = 0; i < encryptedData.length; i += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(encryptedData, i, i + BLOCK_SIZE);
            byte[] decryptedBlock = decryptBlock(block, roundKeys);
            System.arraycopy(decryptedBlock, 0, decrypted, i, BLOCK_SIZE);
        }

        // Remove padding
        return removePadding(decrypted);
    }

    private byte[] padData(byte[] data) {
        int paddingLength = BLOCK_SIZE - (data.length % BLOCK_SIZE);
        byte[] padded = new byte[data.length + paddingLength];
        System.arraycopy(data, 0, padded, 0, data.length);

        // PKCS7 padding
        for (int i = data.length; i < padded.length; i++) {
            padded[i] = (byte) paddingLength;
        }

        return padded;
    }

    private byte[] removePadding(byte[] data) {
        int paddingLength = data[data.length - 1] & 0xFF;
        return Arrays.copyOf(data, data.length - paddingLength);
    }

    private byte[][] generateRoundKeys(byte[] key) {
        int keyWords = keySize / 32;
        int totalWords = (numRounds + 1) * 4;
        byte[][] roundKeys = new byte[totalWords][4];

        // Copy initial key
        for (int i = 0; i < keyWords; i++) {
            System.arraycopy(key, i * 4, roundKeys[i], 0, 4);
        }

        // Generate remaining round keys
        for (int i = keyWords; i < totalWords; i++) {
            byte[] temp = Arrays.copyOf(roundKeys[i - 1], 4);

            if (i % keyWords == 0) {
                temp = rotateWord(temp);
                temp = substituteWord(temp);
                temp[0] ^= RCON[i / keyWords - 1];
            } else if (keyWords > 6 && i % keyWords == 4) {
                temp = substituteWord(temp);
            }

            for (int j = 0; j < 4; j++) {
                roundKeys[i][j] = (byte) (roundKeys[i - keyWords][j] ^ temp[j]);
            }
        }

        return roundKeys;
    }

    private byte[] rotateWord(byte[] word) {
        byte[] rotated = new byte[4];
        rotated[0] = word[1];
        rotated[1] = word[2];
        rotated[2] = word[3];
        rotated[3] = word[0];
        return rotated;
    }

    private byte[] substituteWord(byte[] word) {
        byte[] substituted = new byte[4];
        for (int i = 0; i < 4; i++) {
            substituted[i] = (byte) S_BOX[word[i] & 0xFF];
        }
        return substituted;
    }

    private byte[] encryptBlock(byte[] block, byte[][] roundKeys) {
        byte[] state = Arrays.copyOf(block, BLOCK_SIZE);

        // Initial round
        addRoundKey(state, roundKeys, 0);

        // Main rounds
        for (int round = 1; round < numRounds; round++) {
            subBytes(state);
            shiftRows(state);
            mixColumns(state);
            addRoundKey(state, roundKeys, round);
        }

        // Final round
        subBytes(state);
        shiftRows(state);
        addRoundKey(state, roundKeys, numRounds);

        return state;
    }

    private byte[] decryptBlock(byte[] block, byte[][] roundKeys) {
        byte[] state = Arrays.copyOf(block, BLOCK_SIZE);

        // Initial round
        addRoundKey(state, roundKeys, numRounds);

        // Main rounds
        for (int round = numRounds - 1; round > 0; round--) {
            invShiftRows(state);
            invSubBytes(state);
            addRoundKey(state, roundKeys, round);
            invMixColumns(state);
        }

        // Final round
        invShiftRows(state);
        invSubBytes(state);
        addRoundKey(state, roundKeys, 0);

        return state;
    }

    private void addRoundKey(byte[] state, byte[][] roundKeys, int round) {
        for (int i = 0; i < BLOCK_SIZE; i++) {
            state[i] ^= roundKeys[round * 4 + i / 4][i % 4];
        }
    }

    private void subBytes(byte[] state) {
        for (int i = 0; i < BLOCK_SIZE; i++) {
            state[i] = (byte) S_BOX[state[i] & 0xFF];
        }
    }

    private void invSubBytes(byte[] state) {
        for (int i = 0; i < BLOCK_SIZE; i++) {
            state[i] = (byte) INVERSE_S_BOX[state[i] & 0xFF];
        }
    }

    private void shiftRows(byte[] state) {
        // Row 1: shift left by 1
        byte temp = state[1];
        state[1] = state[5];
        state[5] = state[9];
        state[9] = state[13];
        state[13] = temp;

        // Row 2: shift left by 2
        temp = state[2];
        state[2] = state[10];
        state[10] = temp;
        temp = state[6];
        state[6] = state[14];
        state[14] = temp;

        // Row 3: shift left by 3
        temp = state[3];
        state[3] = state[15];
        state[15] = state[11];
        state[11] = state[7];
        state[7] = temp;
    }

    private void invShiftRows(byte[] state) {
        // Row 1: shift right by 1
        byte temp = state[13];
        state[13] = state[9];
        state[9] = state[5];
        state[5] = state[1];
        state[1] = temp;

        // Row 2: shift right by 2
        temp = state[2];
        state[2] = state[10];
        state[10] = temp;
        temp = state[6];
        state[6] = state[14];
        state[14] = temp;

        // Row 3: shift right by 3
        temp = state[3];
        state[3] = state[7];
        state[7] = state[11];
        state[11] = state[15];
        state[15] = temp;
    }

    // FIX: Updated to pass and use 'int' for calculations
    private void mixColumns(byte[] state) {
        for (int i = 0; i < 4; i++) {
            // Read bytes as unsigned ints for calculation
            int s0 = state[i * 4] & 0xFF;
            int s1 = state[i * 4 + 1] & 0xFF;
            int s2 = state[i * 4 + 2] & 0xFF;
            int s3 = state[i * 4 + 3] & 0xFF;

            // Perform calculations, gmul returns int. Cast final result back to byte.
            state[i * 4] = (byte) (gmul(s0, 2) ^ gmul(s1, 3) ^ s2 ^ s3);
            state[i * 4 + 1] = (byte) (s0 ^ gmul(s1, 2) ^ gmul(s2, 3) ^ s3);
            state[i * 4 + 2] = (byte) (s0 ^ s1 ^ gmul(s2, 2) ^ gmul(s3, 3));
            state[i * 4 + 3] = (byte) (gmul(s0, 3) ^ s1 ^ s2 ^ gmul(s3, 2));
        }
    }

    // FIX: Updated to pass and use 'int' for calculations
    private void invMixColumns(byte[] state) {
        for (int i = 0; i < 4; i++) {
            // Read bytes as unsigned ints for calculation
            int s0 = state[i * 4] & 0xFF;
            int s1 = state[i * 4 + 1] & 0xFF;
            int s2 = state[i * 4 + 2] & 0xFF;
            int s3 = state[i * 4 + 3] & 0xFF;

            // Perform calculations, gmul returns int. Cast final result back to byte.
            state[i * 4] = (byte) (gmul(s0, 14) ^ gmul(s1, 11) ^ gmul(s2, 13) ^ gmul(s3, 9));
            state[i * 4 + 1] = (byte) (gmul(s0, 9) ^ gmul(s1, 14) ^ gmul(s2, 11) ^ gmul(s3, 13));
            state[i * 4 + 2] = (byte) (gmul(s0, 13) ^ gmul(s1, 9) ^ gmul(s2, 14) ^ gmul(s3, 11));
            state[i * 4 + 3] = (byte) (gmul(s0, 11) ^ gmul(s1, 13) ^ gmul(s2, 9) ^ gmul(s3, 14));
        }
    }

    /**
     * Corrected Galois Field Multiplication (GF(2^8)) to prevent lossy conversion.
     * Uses 'int' for intermediate arithmetic.
     */
    private int gmul(int a, int b) {
        int p = 0;
        // Ensure a and b are treated as 8-bit unsigned values (0-255)
        a &= 0xFF;
        b &= 0xFF;

        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) {
                p ^= a;
            }
            // Check if the high bit (x^7 coefficient) is set
            int hi_bit_set = a & 0x80;

            // Left-shift 'a' (multiplication by x)
            a <<= 1;

            // If the high bit was set, reduce modulo M(x) = x^8 + x^4 + x^3 + x + 1 (0x11b)
            if (hi_bit_set != 0) {
                a ^= 0x1b; // XOR with 0x1b
            }

            // Mask to keep 'a' as an 8-bit value for the next loop iteration (important for the check)
            a &= 0xFF;

            // Right-shift 'b' (moving to the next bit)
            b >>= 1;
        }
        return p; // 'p' is the 8-bit result (0-255)
    }
}

