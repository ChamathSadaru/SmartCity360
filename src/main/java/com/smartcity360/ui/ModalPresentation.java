package com.smartcity360.ui;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;

final class ModalPresentation {
    private ModalPresentation() {}

    static void show(JDialog dialog, Component initialFocus) {
        dialog.pack();
        if (dialog.isUndecorated() && dialog.getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            dialog.setShape(new RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 24, 24));
        }
        dialog.setLocationRelativeTo(dialog.getOwner());

        var focusListener = new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                dialog.removeWindowFocusListener(this);
                SwingUtilities.invokeLater(initialFocus::requestFocusInWindow);
            }
        };
        dialog.addWindowFocusListener(focusListener);
        try {
            showWithBackdrop(dialog);
        } finally {
            dialog.removeWindowFocusListener(focusListener);
        }
    }

    private static void showWithBackdrop(JDialog dialog) {
        if (!(dialog.getOwner() instanceof RootPaneContainer owner)) {
            dialog.setVisible(true);
            return;
        }

        Component previous = owner.getGlassPane();
        boolean previousVisible = previous.isVisible();
        var backdrop = new Backdrop();
        owner.setGlassPane(backdrop);
        backdrop.setVisible(true);
        try {
            dialog.setVisible(true);
        } finally {
            backdrop.setVisible(false);
            owner.setGlassPane(previous);
            previous.setVisible(previousVisible);
        }
    }

    private static final class Backdrop extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final Color SHADE = new Color(12, 24, 43, 132);

        @Override
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(SHADE);
            graphics.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
