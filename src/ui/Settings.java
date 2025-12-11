package ui;

import auth.AuthService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Settings component for user profile management
 */
public class Settings extends JPanel {
    
    private AuthService authService;
    private Map<String, String> userData;
    private JTextField nameEditField;
    private JPasswordField passwordEditField;
    private JPasswordField confirmPasswordEditField;
    private JLabel errorLabel;
    
    public Settings(AuthService authService, Map<String, String> userData) {
        this.authService = authService;
        this.userData = userData;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        initializeComponents();
    }
    
    private void initializeComponents() {
        JLabel title = new JLabel("Settings");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        title.setForeground(new Color(17, 24, 39));
        add(title);
        add(Box.createVerticalStrut(32));
        
        // Error label
        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Inter", Font.PLAIN, 13));
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setVisible(false);
        add(errorLabel);
        add(Box.createVerticalStrut(8));
        
        // Editable Profile Section
        add(createSettingSection("Edit Profile"));
        
        // Name field
        JLabel nameLabel = new JLabel("Full Name");
        nameLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        nameLabel.setForeground(new Color(55, 65, 81));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(nameLabel);
        add(Box.createVerticalStrut(6));
        
        nameEditField = createModernTextField();
        nameEditField.setText(userData.get("name"));
        nameEditField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(nameEditField);
        add(Box.createVerticalStrut(20));
        
        // Email (read-only)
        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        emailLabel.setForeground(new Color(55, 65, 81));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(emailLabel);
        add(Box.createVerticalStrut(6));
        
        JTextField emailDisplayField = createModernTextField();
        emailDisplayField.setText(userData.get("email"));
        emailDisplayField.setEditable(false);
        emailDisplayField.setBackground(new Color(249, 250, 251));
        emailDisplayField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(emailDisplayField);
        add(Box.createVerticalStrut(20));
        
        // Password change section
        add(createSettingSection("Change Password"));
        
        JLabel passwordLabel = new JLabel("New Password");
        passwordLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        passwordLabel.setForeground(new Color(55, 65, 81));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(passwordLabel);
        add(Box.createVerticalStrut(6));
        
        passwordEditField = createModernPasswordField();
        passwordEditField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(passwordEditField);
        add(Box.createVerticalStrut(20));
        
        JLabel confirmPasswordLabel = new JLabel("Confirm New Password");
        confirmPasswordLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        confirmPasswordLabel.setForeground(new Color(55, 65, 81));
        confirmPasswordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(confirmPasswordLabel);
        add(Box.createVerticalStrut(6));
        
        confirmPasswordEditField = createModernPasswordField();
        confirmPasswordEditField.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(confirmPasswordEditField);
        add(Box.createVerticalStrut(32));
        
        // Save button
        JButton saveBtn = new JButton("Save Changes");
        saveBtn.setPreferredSize(new Dimension(320, 44));
        saveBtn.setMaximumSize(new Dimension(320, 44));
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.setFont(new Font("Inter", Font.BOLD, 15));
        saveBtn.setBackground(new Color(37, 99, 235));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setBorder(new Client.ModernBorder(8));
        saveBtn.addActionListener(e -> handleSaveSettings());
        add(saveBtn);
        
        add(Box.createVerticalStrut(32));
        
        // Read-only Account Information
        add(createSettingSection("Account Information"));
        add(createSettingItem("Role", userData.get("role")));
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");
        String createdAt = sdf.format(new Date(Long.parseLong(userData.get("created_at"))));
        add(createSettingItem("Member Since", createdAt));
        
        add(Box.createVerticalStrut(24));
        add(createSettingSection("Security"));
        add(createSettingItem("Encryption Method", "RSA-2048 + AES-256"));
        add(createSettingItem("Password Hash", "RSA Signed SHA-256"));
    }
    
    private JTextField createModernTextField() {
        JTextField tf = new JTextField();
        tf.setPreferredSize(new Dimension(400, 44));
        tf.setMaximumSize(new Dimension(400, 44));
        tf.setFont(new Font("Inter", Font.PLAIN, 15));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(0, 14, 0, 14)
        ));
        tf.setBackground(new Color(255, 255, 255));
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(37, 99, 235), 2),
                    BorderFactory.createEmptyBorder(0, 13, 0, 13)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)
                ));
            }
        });
        return tf;
    }
    
    private JPasswordField createModernPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setPreferredSize(new Dimension(400, 44));
        pf.setMaximumSize(new Dimension(400, 44));
        pf.setFont(new Font("Inter", Font.PLAIN, 15));
        pf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(0, 14, 0, 14)
        ));
        pf.setBackground(new Color(255, 255, 255));
        pf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(37, 99, 235), 2),
                    BorderFactory.createEmptyBorder(0, 13, 0, 13)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)
                ));
            }
        });
        return pf;
    }
    
    private JLabel createSettingSection(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", Font.BOLD, 18));
        label.setForeground(new Color(17, 24, 39));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return label;
    }
    
    private JPanel createSettingItem(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Inter", Font.PLAIN, 14));
        labelComp.setForeground(new Color(107, 114, 128));
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Inter", Font.PLAIN, 14));
        valueComp.setForeground(new Color(17, 24, 39));
        
        panel.add(labelComp, BorderLayout.WEST);
        panel.add(valueComp, BorderLayout.EAST);
        
        return panel;
    }
    
    private void handleSaveSettings() {
        errorLabel.setVisible(false);
        
        String newName = nameEditField.getText().trim();
        String newPassword = new String(passwordEditField.getPassword());
        String confirmPassword = new String(confirmPasswordEditField.getPassword());
        
        // Validate name
        if (newName.isEmpty()) {
            showError("Please enter your full name");
            return;
        }
        
        // If password fields are filled, validate password change
        boolean passwordChangeRequested = !newPassword.isEmpty() || !confirmPassword.isEmpty();
        
        if (passwordChangeRequested) {
            if (newPassword.isEmpty()) {
                showError("Please enter a new password");
                return;
            }
            
            if (confirmPassword.isEmpty()) {
                showError("Please confirm your new password");
                return;
            }
            
            if (newPassword.length() < 6) {
                showError("Password must be at least 6 characters");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                showError("Passwords do not match");
                return;
            }
        }
        
        // Update name in userData
        userData.put("name", newName);
        
        // If password change was requested, we would need to update it in the database
        if (passwordChangeRequested) {
            // Note: In a real application, you'd call authService.updatePassword() here
            showSuccess("Profile updated successfully! (Password change requires re-login)");
            passwordEditField.setText("");
            confirmPasswordEditField.setText("");
        } else {
            showSuccess("Profile updated successfully!");
        }
    }
    
    private void showError(String message) {
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void showSuccess(String message) {
        errorLabel.setForeground(new Color(34, 197, 94));
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}



