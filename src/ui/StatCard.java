package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable stat card component for displaying statistics
 */
public class StatCard extends JPanel {
    
    public StatCard(String title, String value, String colorHex) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));
        setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Inter", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(107, 114, 128));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Inter", Font.BOLD, 32));
        valueLabel.setForeground(Color.decode(colorHex));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(titleLabel);
        add(Box.createVerticalStrut(8));
        add(valueLabel);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        super.paintComponent(g);
    }
    
    public void updateValue(String newValue) {
        Component[] comps = getComponents();
        if (comps.length > 1 && comps[1] instanceof JLabel) {
            ((JLabel) comps[1]).setText(newValue);
        }
    }
}



