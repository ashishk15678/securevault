package ui;

import storage.Filesystem;
import crypto.AESEncryption;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Files page component with secure upload/download using AES encryption
 */
public class FilesPage extends JPanel {
    
    private Filesystem filesystem;
    private DefaultTableModel tableModel;
    private JTable table;
    private AESEncryption aes;
    private Map<String, String> fileKeys; // Store AES keys for each file
    private java.util.List<String> filePathsList; // Store file paths in order of table rows
    private String userEmail; // Current user's email for privacy filtering
    
    public FilesPage(Filesystem filesystem, String userEmail) {
        this.filesystem = filesystem;
        this.userEmail = userEmail;
        this.aes = new AESEncryption(256); // AES-256
        this.fileKeys = new HashMap<>();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        initializeComponents();
        loadFiles();
    }
    
    private void initializeComponents() {
        // Title and upload button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("My Files");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        title.setForeground(new Color(17, 24, 39));
        
        JButton uploadBtn = new JButton("📤 Upload File");
        uploadBtn.setFont(new Font("Inter", Font.BOLD, 14));
        uploadBtn.setBackground(new Color(37, 99, 235));
        uploadBtn.setForeground(Color.WHITE);
        uploadBtn.setFocusPainted(false);
        uploadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        uploadBtn.setBorder(new Client.ModernBorder(8));
        uploadBtn.setPreferredSize(new Dimension(140, 40));
        uploadBtn.addActionListener(e -> handleUpload());
        
        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(uploadBtn, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
        
        // Table with additional columns
        String[] columns = {"File Name", "Size", "Encryption", "Uploaded On", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only Actions column is editable
                return column == 4;
            }
        };
        
        table = new JTable(tableModel);
        table.setRowHeight(48);
        table.setFont(new Font("Inter", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Inter", Font.BOLD, 14));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setBackground(new Color(249, 250, 251));
        table.getTableHeader().setForeground(new Color(55, 65, 81));
        
        // Add action buttons to table
        table.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        ButtonEditor buttonEditor = new ButtonEditor(new JCheckBox());
        table.getColumn("Actions").setCellEditor(buttonEditor);
        
        // Make sure clicking on the Actions column triggers the editor
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                
                if (column >= 0 && row >= 0 && table.getColumnName(column).equals("Actions")) {
                    // Start editing to trigger button clicks
                    table.editCellAt(row, column);
                }
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        scroll.setBackground(Color.WHITE);
        
        add(scroll, BorderLayout.CENTER);
    }
    
    private void handleUpload() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Upload");
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            try {
                // Read file
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());
                
                // Generate AES key
                byte[] aesKey = aes.generateKey();
                String keyHex = bytesToHex(aesKey);
                
                // Encrypt file
                byte[] encryptedData = aes.encrypt(fileData, aesKey);
                
                // Store encrypted file
                String fileName = selectedFile.getName();
                String filePath = "/" + System.currentTimeMillis() + "_" + fileName;
                
                Map<String, String> metadata = new HashMap<>();
                metadata.put("original_name", fileName);
                metadata.put("aes_key", keyHex);
                metadata.put("encrypted", "true");
                metadata.put("key_size", "256");
                metadata.put("user_email", userEmail); // Store user email for privacy
                
                boolean success = filesystem.storeFile(filePath, encryptedData, metadata);
                
                if (success) {
                    // Store key for display
                    fileKeys.put(filePath, keyHex);
                    
                    JOptionPane.showMessageDialog(this, 
                        "File uploaded and encrypted successfully!\n\n" +
                        "AES Key (save this for decryption):\n" + keyHex,
                        "Upload Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    loadFiles();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to upload file.",
                        "Upload Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error uploading file: " + e.getMessage(),
                    "Upload Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void handleDownload(int row) {
        try {
            String filePath = getFilePathFromRow(row);
            if (filePath == null) {
                JOptionPane.showMessageDialog(this, "File path not found for this row.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Filesystem.FileMetadata meta = filesystem.getFileMetadata(filePath);
            if (meta == null) {
                JOptionPane.showMessageDialog(this, "File metadata not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Map<String, String> metadata = meta.getMetadata();
            
            // Security check: Verify file belongs to current user
            String fileOwnerEmail = metadata.get("user_email");
            if (fileOwnerEmail == null || !fileOwnerEmail.equals(userEmail)) {
                JOptionPane.showMessageDialog(this, 
                    "Access denied. This file does not belong to you.",
                    "Security Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String keyHex = metadata.get("aes_key");
            
            if (keyHex == null || keyHex.isEmpty()) {
                JOptionPane.showMessageDialog(this, "AES key not found for this file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Get key from user or use stored key
            String userKey = showKeyInputDialog(keyHex, filePath);
            if (userKey == null || userKey.trim().isEmpty()) {
                return; // User cancelled
            }
            
            userKey = userKey.trim().replaceAll("\\s+", "");
            byte[] aesKey = hexToBytes(userKey);
            
            // Retrieve and decrypt file
            byte[] encryptedData = filesystem.retrieveFile(filePath);
            if (encryptedData == null) {
                JOptionPane.showMessageDialog(this, "Failed to retrieve file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            byte[] decryptedData = aes.decrypt(encryptedData, aesKey);
            
            // Save decrypted file
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setDialogTitle("Save Decrypted File");
            String originalName = metadata.get("original_name");
            if (originalName != null) {
                saveChooser.setSelectedFile(new File(originalName));
            }
            
            int result = saveChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File saveFile = saveChooser.getSelectedFile();
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    fos.write(decryptedData);
                    JOptionPane.showMessageDialog(this,
                        "File decrypted and saved successfully!",
                        "Download Success",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                "Invalid AES key format. Please check the key and try again.",
                "Download Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error downloading file: " + e.getMessage(),
                "Download Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void handleShowKey(int row) {
        String filePath = getFilePathFromRow(row);
        if (filePath == null) {
            JOptionPane.showMessageDialog(this, "File path not found for this row.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Filesystem.FileMetadata meta = filesystem.getFileMetadata(filePath);
        if (meta == null) {
            JOptionPane.showMessageDialog(this, "File metadata not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Map<String, String> metadata = meta.getMetadata();
        
        // Security check: Verify file belongs to current user
        String fileOwnerEmail = metadata.get("user_email");
        if (fileOwnerEmail == null || !fileOwnerEmail.equals(userEmail)) {
            JOptionPane.showMessageDialog(this, 
                "Access denied. This file does not belong to you.",
                "Security Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String keyHex = metadata.get("aes_key");
        
        if (keyHex == null || keyHex.isEmpty()) {
            JOptionPane.showMessageDialog(this, "AES key not found for this file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        showKeyDialog(keyHex, filePath);
    }
    
    private void handleVerify(int row) {
        String filePath = getFilePathFromRow(row);
        if (filePath == null) {
            JOptionPane.showMessageDialog(this, "File path not found for this row.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Filesystem.FileMetadata meta = filesystem.getFileMetadata(filePath);
        if (meta == null) {
            JOptionPane.showMessageDialog(this, "File metadata not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Map<String, String> metadata = meta.getMetadata();
        
        // Security check: Verify file belongs to current user
        String fileOwnerEmail = metadata.get("user_email");
        if (fileOwnerEmail == null || !fileOwnerEmail.equals(userEmail)) {
            JOptionPane.showMessageDialog(this, 
                "Access denied. This file does not belong to you.",
                "Security Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String storedKey = metadata.get("aes_key");
        
        if (storedKey == null || storedKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "AES key not found for this file.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get key from user with a better dialog
        JDialog verifyDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Verify File", true);
        verifyDialog.setSize(500, 200);
        verifyDialog.setLocationRelativeTo(this);
        verifyDialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel label = new JLabel("<html>Enter AES Key to verify:<br><small>Stored key: " + storedKey.substring(0, Math.min(32, storedKey.length())) + "...</small></html>");
        label.setFont(new Font("Inter", Font.PLAIN, 14));
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        
        JTextField keyField = new JTextField(storedKey);
        keyField.setFont(new Font("Courier", Font.PLAIN, 12));
        keyField.setPreferredSize(new Dimension(450, 35));
        panel.add(keyField);
        panel.add(Box.createVerticalStrut(15));
        
        final String[] result = new String[1];
        JButton verifyBtn = new JButton("Verify");
        verifyBtn.setBackground(new Color(34, 197, 94));
        verifyBtn.setForeground(Color.WHITE);
        verifyBtn.addActionListener(e -> {
            result[0] = keyField.getText().trim().replaceAll("\\s+", "");
            verifyDialog.dispose();
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            result[0] = null;
            verifyDialog.dispose();
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(verifyBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel);
        
        verifyDialog.add(panel);
        verifyDialog.setVisible(true);
        
        String userKey = result[0];
        if (userKey == null || userKey.isEmpty()) {
            return;
        }
        
        // Try to decrypt to verify
        try {
            byte[] encryptedData = filesystem.retrieveFile(filePath);
            if (encryptedData == null) {
                JOptionPane.showMessageDialog(this,
                    "✗ Verification Failed!\n\n" +
                    "Failed to retrieve file data.",
                    "Verification Failed",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            byte[] aesKey = hexToBytes(userKey);
            byte[] decryptedData = aes.decrypt(encryptedData, aesKey);
            
            // Check if key matches stored key
            boolean keyMatches = userKey.equalsIgnoreCase(storedKey);
            
            if (keyMatches) {
                JOptionPane.showMessageDialog(this,
                    "✓ Verification Successful!\n\n" +
                    "The key matches the stored key and can decrypt the file.\n" +
                    "Decrypted file size: " + formatBytes(decryptedData.length),
                    "Verification Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "⚠ Key Works but Doesn't Match!\n\n" +
                    "The key can decrypt the file, but it doesn't match the stored key.\n" +
                    "Decrypted file size: " + formatBytes(decryptedData.length),
                    "Verification Warning",
                    JOptionPane.WARNING_MESSAGE);
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                "✗ Verification Failed!\n\n" +
                "Invalid key format. Please check the key and try again.\n" +
                "Error: " + e.getMessage(),
                "Verification Failed",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "✗ Verification Failed!\n\n" +
                "The key cannot decrypt this file.\n" +
                "Error: " + e.getMessage(),
                "Verification Failed",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String showKeyInputDialog(String storedKey, String filePath) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Enter AES Key", true);
        dialog.setSize(500, 250);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel label = new JLabel("<html>Enter AES Key for decryption:<br><small>Stored key: " + storedKey + "</small></html>");
        label.setFont(new Font("Inter", Font.PLAIN, 14));
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        
        JTextField keyField = new JTextField(storedKey);
        keyField.setFont(new Font("Courier", Font.PLAIN, 12));
        keyField.setPreferredSize(new Dimension(450, 35));
        panel.add(keyField);
        panel.add(Box.createVerticalStrut(15));
        
        JButton okBtn = new JButton("Use Key");
        okBtn.setBackground(new Color(37, 99, 235));
        okBtn.setForeground(Color.WHITE);
        okBtn.addActionListener(e -> dialog.dispose());
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            keyField.setText("");
            dialog.dispose();
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel);
        
        dialog.add(panel);
        dialog.setVisible(true);
        
        String key = keyField.getText().trim().replaceAll("\\s+", "");
        return key.isEmpty() ? null : key;
    }
    
    private void showKeyDialog(String keyHex, String filePath) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "AES Key", true);
        dialog.setSize(550, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel title = new JLabel("AES Encryption Key");
        title.setFont(new Font("Inter", Font.BOLD, 18));
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        
        JLabel info = new JLabel("<html>Save this key to decrypt your file later:<br><br></html>");
        info.setFont(new Font("Inter", Font.PLAIN, 14));
        panel.add(info);
        
        JTextArea keyArea = new JTextArea(keyHex);
        keyArea.setFont(new Font("Courier", Font.BOLD, 14));
        keyArea.setEditable(false);
        keyArea.setBackground(new Color(249, 250, 251));
        keyArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        keyArea.setLineWrap(true);
        keyArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(keyArea);
        scroll.setPreferredSize(new Dimension(500, 100));
        panel.add(scroll);
        panel.add(Box.createVerticalStrut(15));
        
        JButton copyBtn = new JButton("Copy Key");
        copyBtn.setBackground(new Color(37, 99, 235));
        copyBtn.setForeground(Color.WHITE);
        copyBtn.addActionListener(e -> {
            keyArea.selectAll();
            keyArea.copy();
            JOptionPane.showMessageDialog(dialog, "Key copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(copyBtn);
        buttonPanel.add(closeBtn);
        panel.add(buttonPanel);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private String getFilePathFromRow(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return null;
        }
        
        // Use the stored file paths list
        if (filePathsList != null && row < filePathsList.size()) {
            return filePathsList.get(row);
        }
        
        // Fallback: try to find by filename
        String fileName = (String) tableModel.getValueAt(row, 0);
        List<String> allFiles = getAllFilePaths();
        
        for (String filePath : allFiles) {
            Filesystem.FileMetadata meta = filesystem.getFileMetadata(filePath);
            if (meta != null) {
                Map<String, String> metadata = meta.getMetadata();
                String originalName = metadata.get("original_name");
                
                // Check if original name matches
                if (originalName != null && originalName.equals(fileName)) {
                    return filePath;
                }
                
                // Check if path name matches (after removing timestamp)
                String pathName = filePath.contains("/") 
                    ? filePath.substring(filePath.lastIndexOf("/") + 1) 
                    : filePath;
                if (pathName.contains("_")) {
                    String nameWithoutTimestamp = pathName.substring(pathName.indexOf("_") + 1);
                    if (nameWithoutTimestamp.equals(fileName)) {
                        return filePath;
                    }
                }
            }
        }
        return null;
    }
    
    public void loadFiles() {
        tableModel.setRowCount(0);
        filePathsList = new java.util.ArrayList<>();
        
        if (filesystem != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
            
            // Get all files from metadata (more reliable than listFiles)
            List<String> allFiles = getAllFilePaths();
            
            for (String filePath : allFiles) {
                Filesystem.FileMetadata meta = filesystem.getFileMetadata(filePath);
                if (meta != null) {
                    Map<String, String> metadata = meta.getMetadata();
                    
                    // Privacy: Only show files belonging to the current user
                    String fileOwnerEmail = metadata.get("user_email");
                    if (fileOwnerEmail == null || !fileOwnerEmail.equals(userEmail)) {
                        // Skip files that don't belong to this user
                        continue;
                    }
                    
                    String originalName = metadata.get("original_name");
                    String fileName = originalName != null ? originalName : 
                        (filePath.contains("/") ? filePath.substring(filePath.lastIndexOf("/") + 1) : filePath);
                    
                    // Clean up filename if it has timestamp prefix
                    if (fileName.contains("_") && fileName.matches("^\\d+_.+")) {
                        fileName = fileName.substring(fileName.indexOf("_") + 1);
                    }
                    
                    String size = formatBytes(meta.getSize());
                    String encryption = metadata.get("encrypted") != null && metadata.get("encrypted").equals("true")
                        ? "AES-256" : "None";
                    String date = sdf.format(new Date(meta.getCreated()));
                    
                    // Store file path in the same order as table rows
                    filePathsList.add(filePath);
                    tableModel.addRow(new Object[]{fileName, size, encryption, date, "Actions"});
                }
            }
        }
    }
    
    private List<String> getAllFilePaths() {
        List<String> allFiles = new java.util.ArrayList<>();
        
        // Use searchFiles with empty string to get all files (since every path contains "")
        // This is the most reliable way to get all files from metadata
        try {
            List<String> allPaths = filesystem.searchFiles("");
            if (allPaths != null && !allPaths.isEmpty()) {
                allFiles.addAll(allPaths);
            }
        } catch (Exception e) {
            // Fallback to directory listing
            List<String> rootFiles = filesystem.listFiles("/");
            if (rootFiles != null) {
                allFiles.addAll(rootFiles);
            }
        }
        
        return allFiles;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
    
    // Button renderer and editor for table actions
    class ButtonRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JButton downloadBtn;
        private JButton keyBtn;
        private JButton verifyBtn;
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setOpaque(true);
            
            downloadBtn = createActionButton("Download", new Color(37, 99, 235));
            keyBtn = createActionButton("Show Key", new Color(107, 114, 128));
            verifyBtn = createActionButton("Verify", new Color(34, 197, 94));
            
            add(downloadBtn);
            add(keyBtn);
            add(verifyBtn);
        }
        
        private JButton createActionButton(String text, Color color) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Inter", Font.PLAIN, 11));
            btn.setBackground(color);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setPreferredSize(new Dimension(70, 28));
            return btn;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
    
    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton downloadBtn;
        private JButton keyBtn;
        private JButton verifyBtn;
        private int currentRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setOpaque(true);
            
            downloadBtn = createActionButton("Download", new Color(37, 99, 235));
            keyBtn = createActionButton("Show Key", new Color(107, 114, 128));
            verifyBtn = createActionButton("Verify", new Color(34, 197, 94));
            
            downloadBtn.addActionListener(e -> {
                fireEditingStopped();
                handleDownload(currentRow);
            });
            
            keyBtn.addActionListener(e -> {
                fireEditingStopped();
                handleShowKey(currentRow);
            });
            
            verifyBtn.addActionListener(e -> {
                fireEditingStopped();
                handleVerify(currentRow);
            });
            
            panel.add(downloadBtn);
            panel.add(keyBtn);
            panel.add(verifyBtn);
        }
        
        private JButton createActionButton(String text, Color color) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Inter", Font.PLAIN, 11));
            btn.setBackground(color);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setPreferredSize(new Dimension(70, 28));
            return btn;
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Actions";
        }
    }
}
