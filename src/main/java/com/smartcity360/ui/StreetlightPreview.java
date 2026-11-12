package com.smartcity360.ui;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

final class StreetlightPreview extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final BufferedImage FIXTURE = loadFixture();
    private boolean lit;

    StreetlightPreview() {
        setOpaque(false);
        setPreferredSize(new Dimension(120, 250));
    }

    void setLit(boolean lit) {
        this.lit = lit;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        double scale = Math.min((getWidth() - 12.0) / FIXTURE.getWidth(),
                (getHeight() - 12.0) / FIXTURE.getHeight());
        if (scale <= 0) {
            return;
        }
        var fixture = (Graphics2D) graphics.create();
        fixture.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        fixture.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        fixture.translate((getWidth() - FIXTURE.getWidth() * scale) / 2,
                (getHeight() - FIXTURE.getHeight() * scale) / 2);
        fixture.scale(scale, scale);

        if (lit) {
            paintGlow(fixture);
        }
        fixture.drawImage(FIXTURE, 0, 0, null);
        if (lit) {
            fixture.setColor(new Color(255, 233, 157, 185));
            fixture.fill(new RoundRectangle2D.Double(431, 147, 333, 17, 10, 10));
        }
        fixture.dispose();
    }

    private static void paintGlow(Graphics2D fixture) {
        // Source-pixel coordinates for the generated streetlight diffuser.
        var beam = new Path2D.Double();
        beam.moveTo(414, 170);
        beam.lineTo(786, 170);
        beam.lineTo(856, 920);
        beam.lineTo(183, 920);
        beam.closePath();
        fixture.setPaint(new GradientPaint(595, 170, new Color(255, 197, 74, 132),
                595, 920, new Color(255, 220, 138, 0)));
        fixture.fill(beam);

        fixture.setPaint(new RadialGradientPaint(595, 203, 248,
                new float[] {0f, 1f}, new Color[] {new Color(255, 192, 56, 155), new Color(255, 217, 111, 0)}));
        fixture.fill(new Ellipse2D.Double(347, -45, 496, 496));
    }

    private static BufferedImage loadFixture() {
        try (var stream = StreetlightPreview.class.getResourceAsStream("streetlight.png")) {
            if (stream == null) {
                throw new IllegalStateException("The streetlight image is missing.");
            }
            return ImageIO.read(stream);
        } catch (IOException error) {
            throw new IllegalStateException("Could not load the streetlight image.", error);
        }
    }
}
