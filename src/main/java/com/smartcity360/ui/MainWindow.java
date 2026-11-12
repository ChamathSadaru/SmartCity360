package com.smartcity360.ui;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.function.Supplier;

final class MainWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Color CANVAS = new Color(246, 248, 252);
    static final Color DIVIDER = new Color(225, 231, 241);
    static final Color INK = new Color(22, 34, 57);
    static final Color NAV_TEXT = new Color(63, 79, 105);
    static final Color BLUE = new Color(39, 77, 208);
    static final Color SOFT_BLUE = new Color(235, 241, 255);
    private static final Screen HOME = new Screen("Home", ScreenIcon.Shape.HOME, HomePanel::new);
    private static final Screen[] SCREENS = {
        HOME,
        new Screen("Metering", ScreenIcon.Shape.METER, MeteringPanel::new),
        new Screen("City Zones", ScreenIcon.Shape.ZONES, CityZonesPanel::new),
        new Screen("Citizen Complaints", ScreenIcon.Shape.COMPLAINTS, ComplaintsPanel::new),
        new Screen("Streetlights", ScreenIcon.Shape.LIGHTS, StreetlightsPanel::new),
        new Screen("City Policies", ScreenIcon.Shape.POLICIES, CityPoliciesPanel::new)
    };

    private record Screen(String title, ScreenIcon.Shape icon, Supplier<JPanel> content) {}

    MainWindow() {
        super(DesktopApp.PROJECT_NAME);
        setIconImages(AppIcon.windowImages());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        var cardLayout = new CardLayout();
        var screens = new JPanel(cardLayout);
        screens.setOpaque(false);
        var heading = new JLabel();
        heading.setFont(new Font("Segoe UI", Font.BOLD, 27));
        heading.setForeground(INK);
        heading.setVisible(false);

        var navigation = new JPanel(new GridLayout(0, 1, 0, 8));
        navigation.setOpaque(false);

        var group = new ButtonGroup();
        for (Screen screen : SCREENS) {
            screens.add(screen.content().get(), screen.title());
            var button = new NavigationButton(screen);
            group.add(button);
            navigation.add(button);
            button.addActionListener(event -> {
                heading.setText(screen.title());
                heading.setVisible(screen != HOME);
                cardLayout.show(screens, screen.title());
            });
        }
        group.getElements().nextElement().setSelected(true);

        var workspace = new JPanel(new BorderLayout(0, 20));
        workspace.setBackground(CANVAS);
        workspace.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        workspace.add(heading, BorderLayout.NORTH);
        workspace.add(screens, BorderLayout.CENTER);

        add(sidebar(navigation), BorderLayout.WEST);
        add(workspace, BorderLayout.CENTER);
        setMinimumSize(new Dimension(850, 520));
        setSize(1080, 680);
        setLocationRelativeTo(null);
    }

    private static JPanel sidebar(JPanel navigation) {
        var sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(236, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, DIVIDER),
                BorderFactory.createEmptyBorder(24, 16, 24, 16)));

        var brand = new JLabel(DesktopApp.PROJECT_NAME);
        brand.setForeground(INK);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 19));
        brand.setIcon(AppIcon.atSize(34));
        brand.setIconTextGap(10);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        sidebar.add(brand, BorderLayout.NORTH);

        var navigationArea = new JPanel(new GridBagLayout());
        navigationArea.setOpaque(false);
        var centered = new GridBagConstraints();
        centered.weightx = 1;
        centered.fill = GridBagConstraints.HORIZONTAL;
        navigationArea.add(navigation, centered);
        sidebar.add(navigationArea, BorderLayout.CENTER);
        var stamp = new JLabel("Engineered by ChamathZ", SwingConstants.CENTER);
        stamp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        stamp.setForeground(NAV_TEXT);
        stamp.setVerticalAlignment(SwingConstants.BOTTOM);
        stamp.setPreferredSize(new Dimension(stamp.getPreferredSize().width, brand.getPreferredSize().height));
        sidebar.add(stamp, BorderLayout.SOUTH);
        return sidebar;
    }

    private static final class NavigationButton extends JToggleButton {
        private static final long serialVersionUID = 1L;

        NavigationButton(Screen screen) {
            super(screen.title(), new ScreenIcon(screen.icon(), 18));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setIconTextGap(10);
            setForeground(NAV_TEXT);
            addItemListener(event -> setForeground(isSelected() ? Color.WHITE : NAV_TEXT));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            var shape = (Graphics2D) graphics.create();
            shape.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int height = getHeight() - 2;
            if (isSelected() || getModel().isRollover()) {
                shape.setColor(isSelected() ? BLUE : SOFT_BLUE);
                shape.fillRoundRect(1, 1, getWidth() - 2, height, height, height);
            }
            shape.dispose();
            super.paintComponent(graphics);
        }
    }
}
