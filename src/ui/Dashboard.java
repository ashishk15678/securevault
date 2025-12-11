package ui;

import storage.Filesystem;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dashboard component displaying statistics and overview
 */
public class Dashboard extends JPanel {
    
    private Filesystem filesystem;
    private StatCard filesCard;
    private StatCard storageCard;
    private StatCard lastLoginCard;
    private String userEmail; // Current user's email for privacy filtering
    
    public Dashboard(Filesystem filesystem, String userEmail) {
        this.filesystem = filesystem;
        this.userEmail = userEmail;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        initializeComponents();
        updateStats();
    }
    
    private void initializeComponents() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 24, 24));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        statsPanel.setBackground(Color.WHITE);
        
        filesCard = new StatCard("Total Files", "0", "#2563EB");
        storageCard = new StatCard("Storage Used", "0 B", "#7C3AED");
        lastLoginCard = new StatCard("Last Login", "Just now", "#059669");
        
        statsPanel.add(filesCard);
        statsPanel.add(storageCard);
        statsPanel.add(lastLoginCard);
        
        add(statsPanel, BorderLayout.NORTH);
    }
    
    public void updateStats() {
        if (filesystem != null) {
            // Calculate stats only for current user's files
            int userFileCount = 0;
            long userTotalSize = 0;
            
            try {
                java.util.List<String> allFiles = filesystem.searchFiles("");
                if (allFiles != null) {
                    for (String filePath : allFiles) {
                        Filesystem.FileMetadata meta = filesystem.getFileMetadata(filePath);
                        if (meta != null) {
                            java.util.Map<String, String> metadata = meta.getMetadata();
                            String fileOwnerEmail = metadata.get("user_email");
                            
                            // Only count files belonging to this user
                            if (fileOwnerEmail != null && fileOwnerEmail.equals(userEmail)) {
                                userFileCount++;
                                userTotalSize += meta.getSize();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to total stats if search fails
                Filesystem.StorageStats stats = filesystem.getStorageStats();
                userFileCount = stats.getFileCount();
                userTotalSize = stats.getTotalSize();
            }
            
            filesCard.updateValue(String.valueOf(userFileCount));
            storageCard.updateValue(formatBytes(userTotalSize));
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
            String lastLogin = sdf.format(new Date());
            lastLoginCard.updateValue(lastLogin);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

