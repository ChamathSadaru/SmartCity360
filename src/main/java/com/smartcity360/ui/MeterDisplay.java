package com.smartcity360.ui;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

final class MeterDisplay extends JPanel {
    private static final long serialVersionUID = 1L;
    // Source-pixel coordinates for the wired meter asset.
    private static final int IMAGE_LEFT = 193;
    private static final int IMAGE_WIDTH = 900;
    private static final int LCD_LEFT = 368;
    private static final int LCD_TOP = 177;
    private static final int LCD_WIDTH = 444;
    private static final int LCD_HEIGHT = 196;
    private static final int LED_X = 896;
    private static final int PORT_Y = 501;
    private static final Color OFF = new Color(37, 46, 48);
    private static final Color READY = new Color(102, 208, 82);
    private static final Color AMBER = new Color(255, 173, 44);
    private static final Color CYAN = new Color(54, 209, 248);
    private static final Color VIOLET = new Color(168, 117, 255);
    private static final Color PORT_RED = new Color(249, 64, 62);
    private static final Color PORT_BLUE = new Color(68, 131, 255);
    private static final int STEP_MS = 180;
    private static final int HOLD_MS = 2000;

    private final transient BufferedImage meter = loadMeter();
    private String meterId = "ID —";
    private String reading = "— kWh";
    private boolean ready;
    private boolean bluePort;
    private boolean lcdTextVisible = true;
    private int activeLights;
    private final Timer activityTimer = new Timer(STEP_MS, event -> advanceActivity());
    private final Timer opticalTimer = new Timer(480, event -> {
        bluePort = !bluePort;
        repaint();
    });
    private final Timer lcdTimer = new Timer(300, event -> {
        lcdTextVisible = !lcdTextVisible;
        repaint();
    });

    MeterDisplay() {
        setOpaque(false);
        setPreferredSize(new Dimension(280, 360));
        showPlaceholder("");
        activityTimer.setRepeats(false);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    opticalTimer.start();
                } else {
                    opticalTimer.stop();
                    activityTimer.stop();
                    activeLights = 0;
                }
            }
        });
    }

    void showReading(String id, String kwh) {
        stopBlinking();
        meterId = id;
        reading = kwh + " kWh";
        getAccessibleContext().setAccessibleName("Meter display. " + meterId + ", " + reading);
        repaint();
    }

    void showPlaceholder(String id) {
        stopBlinking();
        meterId = id.isEmpty() ? "ID —" : id;
        reading = "— kWh";
        getAccessibleContext().setAccessibleName(id.isEmpty()
                ? "Meter display. No reading selected." : "Meter display. " + id + ", reading locked.");
        repaint();
    }

    void blinkWhileProcessing(String id) {
        showPlaceholder(id);
        reading = "··· kWh";
        getAccessibleContext().setAccessibleName("Meter display. " + id + ", processing.");
        lcdTimer.restart();
        repaint();
    }

    void cancelProcessing() {
        if (lcdTimer.isRunning()) {
            showPlaceholder(meterId);
        }
    }

    private void stopBlinking() {
        lcdTimer.stop();
        lcdTextVisible = true;
    }

    void setReady(boolean ready) {
        this.ready = ready;
        repaint();
    }

    void playActivity() {
        activeLights = 1;
        activityTimer.setInitialDelay(STEP_MS);
        activityTimer.restart();
        repaint();
    }

    private void advanceActivity() {
        if (activeLights == 3) {
            activeLights = 0;
        } else {
            activeLights++;
            activityTimer.setInitialDelay(activeLights == 3 ? HOLD_MS : STEP_MS);
            activityTimer.restart();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        double scale = Math.min((double) getWidth() / IMAGE_WIDTH, (double) getHeight() / meter.getHeight());
        var display = (Graphics2D) graphics.create();
        display.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        display.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        display.translate((getWidth() - IMAGE_WIDTH * scale) / 2, (getHeight() - meter.getHeight() * scale) / 2);
        display.scale(scale, scale);
        display.translate(-IMAGE_LEFT, 0);

        display.setColor(new Color(192, 208, 170));
        display.fillRect(LCD_LEFT, LCD_TOP, LCD_WIDTH, LCD_HEIGHT);
        display.setColor(new Color(29, 43, 29));
        drawCentered(display, meterId, 247, 39);
        if (lcdTextVisible) {
            drawCentered(display, reading, 325, 70);
        }
        paintIndicators(display);
        display.drawImage(meter, 0, 0, null);
        display.dispose();
    }

    private void paintIndicators(Graphics2D display) {
        paintLight(display, LED_X, 196, 36, 25, ready ? READY : OFF);
        paintLight(display, LED_X, 246, 36, 25, activeLights >= 1 ? AMBER : OFF);
        paintLight(display, LED_X, 296, 36, 25, activeLights >= 2 ? CYAN : OFF);
        paintLight(display, LED_X, 346, 36, 25, activeLights >= 3 ? VIOLET : OFF);
        paintLight(display, 812, PORT_Y, 36, 36, bluePort ? OFF : PORT_RED);
        paintLight(display, 853, PORT_Y, 36, 36, bluePort ? PORT_BLUE : OFF);
    }

    private static void paintLight(Graphics2D display, int x, int y, int width, int height, Color color) {
        int top = y - height / 2;
        display.setPaint(new GradientPaint(x, top, color.brighter(), x, top + height, color.darker()));
        display.fillRect(x - width / 2, top, width, height);
    }

    private static void drawCentered(Graphics2D display, String text, int baseline, int size) {
        var font = new Font(Font.MONOSPACED, Font.BOLD, size);
        int width = display.getFontMetrics(font).stringWidth(text);
        if (width > LCD_WIDTH - 48) {
            font = font.deriveFont((float) size * (LCD_WIDTH - 48) / width);
            width = display.getFontMetrics(font).stringWidth(text);
        }
        display.setFont(font);
        display.drawString(text, LCD_LEFT + (LCD_WIDTH - width) / 2, baseline);
    }

    private static BufferedImage loadMeter() {
        try (var input = MeterDisplay.class.getResourceAsStream("smart-meter-wired-indicators.png")) {
            if (input == null) {
                throw new IllegalStateException("The wired meter image is missing.");
            }
            return ImageIO.read(input);
        } catch (IOException error) {
            throw new IllegalStateException("Could not load the wired meter image.", error);
        }
    }
}
