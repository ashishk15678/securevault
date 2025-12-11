package ui;

import auth.AuthService;
import db.Database;
import crypto.RSAEncryption;
import storage.Filesystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * Main ClientService window with sidebar navigation and content area
 * Uses separate components: Dashboard, FilesPage, and Settings
 */
public class ClientService {

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainContent;
    private AuthService authService;
    private Map<String, String> userData;
    private String token;
    private Filesystem filesystem;
    private JLabel pageTitleLabel;
    
    // Component instances
    private Dashboard dashboard;
    private FilesPage filesPage;
    private Settings settings;

    public ClientService(AuthService authService, Map<String, String> userData, String token) {
        this.authService = authService;
        this.userData = userData;
        this.token = token;
        this.filesystem = new Filesystem("./storage");
    }

    public void init() {
        frame = new JFrame("SecureVault Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JPanel sidebar = createSidebar();
        JPanel contentWrapper = createContentWrapper();

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(contentWrapper, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new Client.RoundedPanel(0);
        sidebar.setPreferredSize(new Dimension(260, 900));
        sidebar.setBackground(new Color(249, 250, 251));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(32, 24, 32, 24));

        // Logo
        JLabel logo = new JLabel("🔐 SecureVault");
        logo.setFont(new Font("Inter", Font.BOLD, 22));
        logo.setForeground(new Color(17, 24, 39));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(48));

        // Navigation buttons
        JButton dashboardBtn = createNavButton("🏠 Dashboard", true);
        JButton filesBtn = createNavButton("📁 My Files", false);
        JButton settingsBtn = createNavButton("⚙️ Settings", false);

        dashboardBtn.addActionListener(e -> {
            setActiveNav(dashboardBtn);
            pageTitleLabel.setText("Dashboard");
            cardLayout.show(mainContent, "dashboard");
            if (dashboard != null) {
                dashboard.updateStats();
            }
        });
        
        filesBtn.addActionListener(e -> {
            setActiveNav(filesBtn);
            pageTitleLabel.setText("My Files");
            cardLayout.show(mainContent, "files");
            if (filesPage != null) {
                filesPage.loadFiles();
            }
        });
        
        settingsBtn.addActionListener(e -> {
            setActiveNav(settingsBtn);
            pageTitleLabel.setText("Settings");
            cardLayout.show(mainContent, "settings");
        });

        sidebar.add(dashboardBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(filesBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(settingsBtn);

        sidebar.add(Box.createVerticalGlue());

        // User info
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setOpaque(false);
        userPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        JLabel userName = new JLabel(userData.get("name"));
        userName.setFont(new Font("Inter", Font.BOLD, 14));
        userName.setForeground(new Color(17, 24, 39));
        
        JLabel userEmail = new JLabel(userData.get("email"));
        userEmail.setFont(new Font("Inter", Font.PLAIN, 12));
        userEmail.setForeground(new Color(107, 114, 128));
        
        JPanel userInfo = new JPanel();
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        userInfo.setOpaque(false);
        userInfo.add(userName);
        userInfo.add(userEmail);
        
        userPanel.add(userInfo, BorderLayout.CENTER);
        sidebar.add(userPanel);

        // Logout button
        JButton logoutBtn = new JButton("Sign Out");
        logoutBtn.setMaximumSize(new Dimension(212, 40));
        logoutBtn.setFont(new Font("Inter", Font.BOLD, 14));
        logoutBtn.setBackground(new Color(239, 68, 68));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setBorder(new Client.ModernBorder(8));
        logoutBtn.addActionListener(e -> {
            authService.logout(token);
            frame.dispose();
            // Restart login
            Database db = new Database("./data");
            RSAEncryption rsa = new RSAEncryption();
            AuthService auth = new AuthService(db, rsa, 24L * 60 * 60 * 1000);
            Client client = new Client(auth);
            client.init();
        });

        sidebar.add(Box.createVerticalStrut(16));
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private JPanel createContentWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(255, 255, 255));

        // Top bar
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)),
            BorderFactory.createEmptyBorder(20, 32, 20, 32)
        ));

        pageTitleLabel = new JLabel("Dashboard");
        pageTitleLabel.setFont(new Font("Inter", Font.BOLD, 28));
        pageTitleLabel.setForeground(new Color(17, 24, 39));

        // User avatar
        JLabel avatar = new JLabel("👤");
        avatar.setFont(new Font("Inter", Font.PLAIN, 32));
        avatar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        topbar.add(pageTitleLabel, BorderLayout.WEST);
        topbar.add(avatar, BorderLayout.EAST);

        cardLayout = new CardLayout();
        mainContent = new JPanel(cardLayout);
        mainContent.setBackground(Color.WHITE);

        // Initialize components
        dashboard = new Dashboard(filesystem, userData.get("email")); // Pass user email for privacy
        filesPage = new FilesPage(filesystem, userData.get("email")); // Pass user email for privacy
        settings = new Settings(authService, userData);

        mainContent.add(dashboard, "dashboard");
        mainContent.add(filesPage, "files");
        mainContent.add(settings, "settings");

        wrapper.add(topbar, BorderLayout.NORTH);
        wrapper.add(mainContent, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton createNavButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(212, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFont(new Font("Inter", Font.PLAIN, 15));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new Client.ModernBorder(10));
        
        if (active) {
            btn.setBackground(new Color(239, 246, 255));
            btn.setForeground(new Color(37, 99, 235));
        } else {
            btn.setBackground(new Color(249, 250, 251));
            btn.setForeground(new Color(55, 65, 81));
        }
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(new Color(239, 246, 255))) {
                    btn.setBackground(new Color(243, 244, 246));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(new Color(239, 246, 255))) {
                    btn.setBackground(new Color(249, 250, 251));
                }
            }
        });
        
        return btn;
    }

    private void setActiveNav(JButton activeBtn) {
        // Reset all nav buttons
        for (Component comp : activeBtn.getParent().getComponents()) {
            if (comp instanceof JButton && comp != activeBtn) {
                JButton btn = (JButton) comp;
                if (btn.getText().contains("Dashboard") || btn.getText().contains("Files") || btn.getText().contains("Settings")) {
                    btn.setBackground(new Color(249, 250, 251));
                    btn.setForeground(new Color(55, 65, 81));
                }
            }
        }
        // Set active
        activeBtn.setBackground(new Color(239, 246, 255));
        activeBtn.setForeground(new Color(37, 99, 235));
    }
}
