package com.smartcity360.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

record ScreenIcon(Shape shape, int size) implements Icon {
    enum Shape { HOME, METER, ZONES, BUILDING, COMPLAINTS, LIGHTS, POLICIES, AUTH, COMPRESS, ENCRYPT }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        var icon = (Graphics2D) graphics.create();
        icon.translate(x, y);
        icon.scale(size / 24.0, size / 24.0);
        icon.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icon.setColor(component.getForeground());
        icon.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (shape) {
            case HOME -> {
                icon.drawPolyline(new int[] {3, 12, 21}, new int[] {11, 4, 11}, 3);
                icon.drawPolyline(new int[] {5, 5, 19, 19}, new int[] {10, 20, 20, 10}, 4);
                icon.drawPolyline(new int[] {10, 10, 14, 14}, new int[] {20, 14, 14, 20}, 4);
            }
            case METER -> {
                icon.drawArc(3, 4, 18, 18, 0, 180);
                icon.drawLine(3, 13, 3, 17);
                icon.drawLine(21, 13, 21, 17);
                icon.drawLine(3, 17, 21, 17);
                icon.drawLine(12, 13, 16, 9);
                icon.fillOval(10, 11, 4, 4);
            }
            case ZONES -> {
                icon.drawPolygon(new int[] {12, 21, 12, 3}, new int[] {4, 9, 14, 9}, 4);
                icon.drawPolyline(new int[] {3, 12, 21}, new int[] {13, 18, 13}, 3);
                icon.drawPolyline(new int[] {3, 12, 21}, new int[] {17, 22, 17}, 3);
            }
            case BUILDING -> {
                icon.drawRoundRect(5, 3, 14, 18, 2, 2);
                icon.drawLine(9, 8, 10, 8);
                icon.drawLine(14, 8, 15, 8);
                icon.drawLine(9, 12, 10, 12);
                icon.drawLine(14, 12, 15, 12);
                icon.drawPolyline(new int[] {10, 10, 14, 14}, new int[] {21, 16, 16, 21}, 4);
            }
            case COMPLAINTS -> {
                icon.drawPolygon(new int[] {4, 20, 20, 10, 4}, new int[] {5, 5, 16, 16, 20}, 5);
                icon.drawLine(8, 9, 16, 9);
                icon.drawLine(8, 12, 13, 12);
            }
            case LIGHTS -> {
                icon.drawLine(8, 21, 8, 7);
                icon.drawLine(5, 21, 11, 21);
                icon.drawArc(8, 4, 10, 6, 0, 180);
                icon.drawLine(16, 8, 20, 8);
                icon.drawLine(18, 12, 18, 14);
                icon.drawLine(21, 11, 22, 12);
            }
            case POLICIES -> {
                icon.drawRoundRect(5, 3, 14, 18, 3, 3);
                icon.drawLine(9, 8, 15, 8);
                icon.drawLine(9, 12, 15, 12);
                icon.drawLine(9, 16, 13, 16);
            }
            case AUTH -> {
                icon.drawPolygon(new int[] {12, 20, 19, 12, 5, 4}, new int[] {3, 6, 15, 21, 15, 6}, 6);
                icon.drawPolyline(new int[] {8, 11, 16}, new int[] {12, 15, 9}, 3);
            }
            case COMPRESS -> {
                icon.drawLine(4, 4, 9, 9);
                icon.drawPolyline(new int[] {5, 9, 9}, new int[] {9, 9, 5}, 3);
                icon.drawLine(20, 20, 15, 15);
                icon.drawPolyline(new int[] {19, 15, 15}, new int[] {15, 15, 19}, 3);
                icon.drawLine(4, 20, 9, 15);
                icon.drawPolyline(new int[] {5, 9, 9}, new int[] {15, 15, 19}, 3);
                icon.drawLine(20, 4, 15, 9);
                icon.drawPolyline(new int[] {19, 15, 15}, new int[] {9, 9, 5}, 3);
            }
            case ENCRYPT -> {
                icon.drawRoundRect(5, 10, 14, 11, 3, 3);
                icon.drawArc(8, 3, 8, 10, 0, 180);
                icon.drawLine(8, 8, 8, 10);
                icon.drawLine(16, 8, 16, 10);
                icon.drawLine(12, 14, 12, 17);
            }
        }
        icon.dispose();
    }
}
