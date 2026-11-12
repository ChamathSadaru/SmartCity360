package com.smartcity360.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class RoundedPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final Color outline;
    private final int arc;

    RoundedPanel(Color fill, Color outline, int arc) {
        setOpaque(false);
        setBackground(fill);
        this.outline = outline;
        this.arc = arc;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var shape = (Graphics2D) graphics.create();
        shape.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        shape.setColor(getBackground());
        shape.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        shape.setColor(outline);
        shape.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        shape.dispose();
    }
}
