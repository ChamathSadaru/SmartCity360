package com.smartcity360.ui;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

final class AppIcon {
    private static final BufferedImage SOURCE = load();

    private AppIcon() {}

    static List<Image> windowImages() {
        if (SOURCE == null) {
            return List.of();
        }
        return IntStream.of(16, 24, 32, 48, 64, 128, 256)
                .mapToObj(AppIcon::scale)
                .toList();
    }

    static Icon atSize(int size) {
        return SOURCE == null ? null : new ImageIcon(scale(size));
    }

    private static Image scale(int size) {
        return SOURCE.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING);
    }

    private static BufferedImage load() {
        var resource = AppIcon.class.getResource("app-icon.png");
        if (resource == null) {
            return null;
        }
        try {
            return ImageIO.read(resource);
        } catch (IOException exception) {
            return null;
        }
    }
}
