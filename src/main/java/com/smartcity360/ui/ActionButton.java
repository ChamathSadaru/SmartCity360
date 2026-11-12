package com.smartcity360.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.SOFT_BLUE;

final class ActionButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final boolean primary;

    ActionButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setForeground(primary ? Color.WHITE : BLUE);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        var shape = (Graphics2D) graphics.create();
        shape.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int height = getHeight() - 2;
        var pill = new RoundRectangle2D.Float(1, 1, getWidth() - 2, height, height, height);
        shape.setColor(primary && isEnabled() ? BLUE
                : getModel().isRollover() && isEnabled() ? SOFT_BLUE : Color.WHITE);
        shape.fill(pill);
        shape.setColor(isEnabled() ? BLUE : DIVIDER);
        shape.draw(pill);
        shape.dispose();
        super.paintComponent(graphics);
    }
}
