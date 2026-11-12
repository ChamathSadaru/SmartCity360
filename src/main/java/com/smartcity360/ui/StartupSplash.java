package com.smartcity360.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

final class StartupSplash extends JWindow {
    private static final long serialVersionUID = 1L;
    private final JProgressBar progress = new SplashProgressBar();

    StartupSplash() {
        var content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createLineBorder(MainWindow.DIVIDER));
        content.setPreferredSize(new Dimension(600, 340));

        var introduction = new JPanel(new GridBagLayout());
        introduction.setOpaque(false);

        var row = new GridBagConstraints();
        row.gridx = 0;
        row.insets = new Insets(0, 0, 12, 0);
        introduction.add(new JLabel(AppIcon.atSize(68)), row);

        row.gridy = 1;
        row.insets = new Insets(0, 0, 5, 0);
        introduction.add(label("OODP II PROJECT", 12, Font.BOLD, MainWindow.BLUE), row);

        row.gridy++;
        row.insets = new Insets(0, 0, 6, 0);
        introduction.add(label(DesktopApp.PROJECT_NAME, 44, Font.BOLD, MainWindow.INK), row);

        row.gridy++;
        row.insets = new Insets(0, 0, 20, 0);
        introduction.add(label("Smart City Management Platform", 15, Font.PLAIN, MainWindow.NAV_TEXT), row);

        row.gridy++;
        row.insets = new Insets(0, 0, 0, 0);
        introduction.add(progress, row);
        content.add(introduction, BorderLayout.CENTER);

        var tagline = label("Original Architecture & Implementation · All Rights Reserved",
                12, Font.PLAIN, MainWindow.NAV_TEXT);
        tagline.setOpaque(true);
        tagline.setBackground(new Color(246, 248, 252));
        tagline.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.DIVIDER),
                BorderFactory.createEmptyBorder(14, 12, 14, 12)));
        content.add(tagline, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);
    }

    void setProgress(int value) {
        progress.setValue(value);
    }

    private static JLabel label(String text, int size, int style, Color color) {
        var label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private static final class SplashProgressBar extends JProgressBar {
        private static final long serialVersionUID = 1L;

        SplashProgressBar() {
            super(0, 100);
            setPreferredSize(new Dimension(230, 8));
            setBorderPainted(false);
            getAccessibleContext().setAccessibleName("Startup progress");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            var bar = (Graphics2D) graphics.create();
            bar.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int height = getHeight();
            bar.setColor(MainWindow.SOFT_BLUE);
            bar.fillRoundRect(0, 0, getWidth(), height, height, height);
            bar.setColor(MainWindow.BLUE);
            int fill = (int) Math.round(getWidth() * getPercentComplete());
            bar.fillRoundRect(0, 0, fill, height, height, height);
            bar.dispose();
        }
    }
}
