package com.smartcity360.ui;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

final class HomePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final URI PORTFOLIO = URI.create("https://chamathz.com");

    HomePanel() {
        super(new BorderLayout());
        setOpaque(false);

        var card = new RoundedPanel(Color.WHITE, MainWindow.DIVIDER, 24);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 16, 24));
        add(card);

        var title = new JLabel("Welcome to " + DesktopApp.PROJECT_NAME);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(MainWindow.INK);
        title.setAlignmentX(CENTER_ALIGNMENT);

        var hint = new JLabel("Choose a city service from the sidebar.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        hint.setForeground(MainWindow.NAV_TEXT);
        hint.setAlignmentX(CENTER_ALIGNMENT);

        var header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(hint);
        card.add(header, BorderLayout.NORTH);
        card.add(new CityIllustration(), BorderLayout.CENTER);

        var link = new JButton("<html><u>Developed by ChamathZ</u></html>");
        link.setFont(new Font("Segoe UI", Font.BOLD, 13));
        link.setForeground(MainWindow.BLUE);
        link.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        link.setContentAreaFilled(false);
        link.setFocusPainted(false);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setToolTipText("Open chamathz.com in your browser");
        link.getAccessibleContext().setAccessibleName("Developed by ChamathZ");
        link.addActionListener(event -> openPortfolio());

        var footer = Box.createHorizontalBox();
        footer.add(Box.createHorizontalGlue());
        footer.add(link);
        footer.add(Box.createHorizontalGlue());
        card.add(footer, BorderLayout.SOUTH);
    }

    private void openPortfolio() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(PORTFOLIO);
                return;
            }
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            // The user can still open the address from the message below.
        }
        JOptionPane.showMessageDialog(this, "Open https://chamathz.com in your browser.",
                "Browser unavailable", JOptionPane.INFORMATION_MESSAGE);
    }

    private static final class CityIllustration extends JComponent {
        private static final long serialVersionUID = 1L;
        private final transient BufferedImage image = loadImage();

        private static BufferedImage loadImage() {
            var resource = HomePanel.class.getResource("welcome-city.png");
            if (resource == null) {
                return null;
            }
            try {
                return ImageIO.read(resource);
            } catch (IOException exception) {
                return null;
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null || getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            double scale = Math.min(Math.min(getWidth(), 660) / (double) image.getWidth(),
                    getHeight() / (double) image.getHeight());
            int width = (int) Math.round(image.getWidth() * scale);
            int height = (int) Math.round(image.getHeight() * scale);
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;

            var canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            canvas.clip(new RoundRectangle2D.Double(x, y, width, height, 24, 24));
            canvas.drawImage(image, x, y, width, height, null);
            canvas.dispose();
        }
    }
}
