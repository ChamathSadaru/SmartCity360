package com.smartcity360.ui;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class DesktopApp {
    static final String PROJECT_NAME = "SmartCity360";
    private static final int SPLASH_DURATION_MS = 2200;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DesktopApp::showSplash);
    }

    private static void showSplash() {
        var splash = new StartupSplash();
        splash.setVisible(true);
        long startedAt = System.nanoTime();
        var timer = new Timer(25, event -> {
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            splash.setProgress((int) Math.min(100, elapsedMillis * 100 / SPLASH_DURATION_MS));
            if (elapsedMillis >= SPLASH_DURATION_MS) {
                ((Timer) event.getSource()).stop();
                try {
                    new MainWindow().setVisible(true);
                } finally {
                    splash.dispose();
                }
            }
        });
        timer.start();
    }
}
