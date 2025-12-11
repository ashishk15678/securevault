package ui;

import auth.AuthService;
import db.Database;
import crypto.RSAEncryption;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

public class Client {

    private boolean isRegister = false;
    private JFrame frame;
    private AuthService authService;
    private JLabel errorLabel;
    private JLabel subtitle;
    private JButton actionBtn;
    private JLabel switchLabel;
    private JLabel nameLabel;
    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passField;
    private JLabel confirmLabel;
    private JPasswordField confirmField;
    private JPanel card;

    public Client(AuthService authService) {
        this.authService = authService;
    }

    public void init() {
        SwingUtilities.invokeLater(this::createUI);
    }

    private void createUI() {
        frame = new JFrame("SecureVault - Authentication");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(480, 680);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // Clerk.com style background - very light gray/white
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(250, 251, 252));
        frame.add(root);

        // Main card with shadow effect
        card = new RoundedPanel(16);
        card.setPreferredSize(new Dimension(400, isRegister ? 580 : 500));
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 40, 48, 40));

        // Logo/Title section
        JLabel logo = new JLabel("🔐 SecureVault");
        logo.setFont(new Font("Inter", Font.BOLD, 28));
        logo.setForeground(new Color(17, 24, 39));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitle = new JLabel(isRegister ? "Create your account" : "Welcome back");
        subtitle.setFont(new Font("Inter", Font.PLAIN, 15));
        subtitle.setForeground(new Color(107, 114, 128));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(8));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(32));

        // Error label
        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Inter", Font.PLAIN, 13));
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setVisible(false);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(8));

        // Form fields
        nameLabel = new JLabel("Full Name");
        nameLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        nameLabel.setForeground(new Color(55, 65, 81));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameField = createModernTextField();
        nameLabel.setVisible(true);
        nameField.setVisible(true);

        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        emailLabel.setForeground(new Color(55, 65, 81));
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField = createModernTextField();

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        passLabel.setForeground(new Color(55, 65, 81));
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passField = createModernPasswordField();

        confirmLabel = new JLabel("Confirm Password");
        confirmLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        confirmLabel.setForeground(new Color(55, 65, 81));
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmField = createModernPasswordField();
        confirmLabel.setVisible(false);
        confirmField.setVisible(false);

        // Primary button - Clerk.com style
        actionBtn = new JButton(isRegister ? "Create Account" : "Sign In");
        actionBtn.setPreferredSize(new Dimension(320, 44));
        actionBtn.setMaximumSize(new Dimension(320, 44));
        actionBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionBtn.setFont(new Font("Inter", Font.BOLD, 15));
        actionBtn.setBackground(new Color(37, 99, 235));
        actionBtn.setForeground(Color.WHITE);
        actionBtn.setFocusPainted(false);
        actionBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actionBtn.setBorder(new ModernBorder(8));

        // Switch link
        switchLabel = new JLabel(isRegister ? "Already have an account? Sign in" : "Don't have an account? Sign up");
        switchLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        switchLabel.setForeground(new Color(37, 99, 235));
        switchLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        switchLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isRegister = !isRegister;
                subtitle.setText(isRegister ? "Create your account" : "Welcome back");
                actionBtn.setText(isRegister ? "Create Account" : "Sign In");
                switchLabel.setText(isRegister ? "Already have an account? Sign in" : "Don't have an account? Sign up");

                nameLabel.setVisible(isRegister);
                nameField.setVisible(isRegister);
                confirmLabel.setVisible(isRegister);
                confirmField.setVisible(isRegister);

                errorLabel.setVisible(false);
                card.setPreferredSize(new Dimension(400, isRegister ? 580 : 500));
                frame.pack();
                frame.repaint();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                switchLabel.setForeground(new Color(29, 78, 216));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                switchLabel.setForeground(new Color(37, 99, 235));
            }
        });

        // Button hover effect
        actionBtn.addMouseListener(new MouseAdapter() {
            Color base = new Color(37, 99, 235);
            Color hover = new Color(29, 78, 216);
            @Override
            public void mouseEntered(MouseEvent e) {
                actionBtn.setBackground(hover);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                actionBtn.setBackground(base);
            }
        });

        // Action button handler
        actionBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword());
            String confirm = new String(confirmField.getPassword());

            if (isRegister) {
                handleRegister(name, email, password, confirm);
            } else {
                handleLogin(email, password);
            }
        });

        // Layout
        if (isRegister) {
            card.add(nameLabel);
            card.add(Box.createVerticalStrut(6));
            card.add(nameField);
            card.add(Box.createVerticalStrut(20));
        }
        card.add(emailLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(emailField);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(nameField);

        card.add(Box.createVerticalStrut(20));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(passField);
        card.add(confirmLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(confirmField);

        if (isRegister) {
            card.add(Box.createVerticalStrut(20));
            card.add(confirmLabel);
            card.add(Box.createVerticalStrut(6));
            card.add(confirmField);
        }
        card.add(Box.createVerticalStrut(32));
        card.add(actionBtn);
        card.add(Box.createVerticalStrut(24));
        card.add(switchLabel);

        root.add(card);
        frame.setVisible(true);
    }

    private JTextField createModernTextField() {
        JTextField tf = new JTextField();
        tf.setPreferredSize(new Dimension(320, 44));
        tf.setMaximumSize(new Dimension(320, 44));
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
        pf.setPreferredSize(new Dimension(320, 44));
        pf.setMaximumSize(new Dimension(320, 44));
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

    private void handleRegister(String name, String email, String password, String confirm) {
        errorLabel.setVisible(false);
        
        // Validate all fields are filled
        if (name == null || name.trim().isEmpty()) {
            showError("Please enter your full name");
            return;
        }
        
        if (email == null || email.trim().isEmpty()) {
            showError("Please enter your email address");
            return;
        }
        
        if (password == null || password.isEmpty()) {
            showError("Please enter a password");
            return;
        }
        
        if (confirm == null || confirm.isEmpty()) {
            showError("Please confirm your password");
            return;
        }
        
        // Validate email format (basic check)
        if (!email.contains("@") || !email.contains(".")) {
            showError("Please enter a valid email address");
            return;
        }
        
        // Validate password length
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }
        
        // Validate passwords match
        if (!password.equals(confirm)) {
            showError("Passwords do not match");
            return;
        }
        
        // Attempt registration
        boolean success = authService.createUser(name.trim(), email.trim(), password, AuthService.Role.CLIENT);
        if (success) {
            showSuccess("Registration successful! Please sign in.");
            // Switch to login mode after a short delay
            Timer timer = new Timer(2000, e -> {
                isRegister = false;
                // Update UI to show login form
                subtitle.setText("Welcome back");
                actionBtn.setText("Sign In");
                switchLabel.setText("Don't have an account? Sign up");
                nameLabel.setVisible(false);
                nameField.setVisible(false);
                nameField.setText("");
                confirmLabel.setVisible(false);
                confirmField.setVisible(false);
                confirmField.setText("");
                passField.setText("");
                emailField.setText("");
                errorLabel.setVisible(false);
                card.setPreferredSize(new Dimension(400, 500));
                frame.pack();
                frame.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            showError("Registration failed. Email may already be registered.");
        }
    }

    private void handleLogin(String email, String password) {
        errorLabel.setVisible(false);
        
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }
        
        String token = authService.login(email, password);
        if (token != null) {
            // Get user info
            java.util.Map<String, String> user = authService.getUserByEmail(email);
            if (user != null) {
                // Hide login window and show ClientService
                frame.setVisible(false);
                frame.dispose();
                
                // Show ClientService with user data
                ClientService clientService = new ClientService(authService, user, token);
                clientService.init();
            }
        } else {
            showError("Invalid email or password");
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

    // Rounded panel with shadow
    static class RoundedPanel extends JPanel {
        private int radius;
        public RoundedPanel(int radius) { 
            this.radius = radius; 
            setOpaque(false); 
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fillRoundRect(4, 4, getWidth(), getHeight(), radius, radius);
            
            // Draw panel
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
        }
    }

    // Modern border for buttons
    static class ModernBorder implements Border {
        private int radius;
        public ModernBorder(int radius) { this.radius = radius; }
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
        public boolean isBorderOpaque() { return false; }
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            // No border for modern look
        }
    }

    public static void main(String[] args) {
        // For testing only
        Database db = new Database("./data");
        RSAEncryption rsa = new RSAEncryption();
        AuthService auth = new AuthService(db, rsa, 24L * 60 * 60 * 1000);
        new Client(auth).init();
    }
}
