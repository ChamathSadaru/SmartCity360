package com.smartcity360.ui;

import com.smartcity360.zone.CityComponent;
import com.smartcity360.zone.SampleCity;
import com.smartcity360.zone.ZoneGroup;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;
import static com.smartcity360.ui.MainWindow.SOFT_BLUE;

final class CityZonesPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color METRIC_FILL = new Color(247, 249, 253);
    private static final Color POWER_ON = new Color(21, 113, 78);
    private static final Color POWER_PARTIAL = new Color(148, 84, 15);
    private static final Color POWER_OFF = new Color(169, 42, 54);
    private final JTree tree = new JTree(treeNode(SampleCity.create()));
    private final JLabel nodeType = label("", 13, Font.PLAIN, NAV_TEXT);
    private final JLabel nodeName = label("", 24, Font.BOLD, INK);
    private final JLabel usage = label("", 28, Font.BOLD, INK);
    private final JLabel powered = label("", 28, Font.BOLD, POWER_ON);
    private final ActionButton shutdown = new ActionButton("Shutdown Power", true);
    private final ActionButton restore = new ActionButton("Restore Power", false);

    CityZonesPanel() {
        super(new GridLayout(1, 2, 14, 0));
        setName("City Zones");
        setOpaque(false);
        add(treeCard());
        add(summaryCard());

        tree.addTreeSelectionListener(event -> updateSummary());
        shutdown.addActionListener(event -> applyPower(false));
        restore.addActionListener(event -> applyPower(true));
        tree.setSelectionRow(0);
    }

    private JPanel treeCard() {
        var card = card();
        card.add(label("Zone hierarchy", 19, Font.BOLD, INK), BorderLayout.NORTH);

        tree.setBackground(Color.WHITE);
        tree.setRowHeight(33);
        tree.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tree.setCellRenderer(new ZoneRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getAccessibleContext().setAccessibleName("City zone hierarchy");
        for (int row = 0; row < tree.getRowCount(); row++) {
            tree.expandRow(row);
        }

        var scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel summaryCard() {
        var card = card();
        var heading = new JPanel(new BorderLayout(0, 3));
        heading.setOpaque(false);
        heading.add(nodeType, BorderLayout.NORTH);
        heading.add(nodeName, BorderLayout.CENTER);
        card.add(heading, BorderLayout.NORTH);

        var metrics = new JPanel(new GridLayout(2, 1, 0, 10));
        metrics.setOpaque(false);
        metrics.add(metric("Recorded usage", usage));
        metrics.add(metric("Buildings powered", powered));
        var metricArea = new JPanel(new BorderLayout());
        metricArea.setOpaque(false);
        metricArea.add(metrics, BorderLayout.NORTH);
        card.add(metricArea, BorderLayout.CENTER);

        var actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        actions.add(shutdown);
        actions.add(restore);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void updateSummary() {
        var node = selectedZone();
        if (node == null) {
            nodeType.setText("Zone");
            nodeName.setText("Select a zone");
            usage.setText("— kWh");
            powered.setText("—");
            powered.setForeground(NAV_TEXT);
            shutdown.setEnabled(false);
            restore.setEnabled(false);
            return;
        }
        int total = node.buildingCount();
        int on = node.poweredBuildingCount();
        nodeType.setText(node instanceof ZoneGroup zone ? zoneType(zone) : "Building");
        nodeName.setText(node.name());
        usage.setText(String.format(Locale.ROOT, "%.1f kWh", node.reportUsage()));
        powered.setText(on + " / " + total);
        powered.setForeground(on == total ? POWER_ON : on == 0 ? POWER_OFF : POWER_PARTIAL);
        shutdown.setEnabled(on > 0);
        restore.setEnabled(on < total);
    }

    private void applyPower(boolean on) {
        var node = selectedZone();
        if (node == null) {
            return;
        }
        if (on) {
            node.restorePower();
        } else {
            node.shutdownPower();
        }
        updateSummary();
        var model = (DefaultTreeModel) tree.getModel();
        var selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        var affected = selected.depthFirstEnumeration();
        while (affected.hasMoreElements()) {
            var item = (DefaultMutableTreeNode) affected.nextElement();
            if (item.isLeaf()) {
                model.nodeChanged(item);
            }
        }
    }

    private CityComponent selectedZone() {
        return tree.getLastSelectedPathComponent() instanceof DefaultMutableTreeNode node
                ? (CityComponent) node.getUserObject() : null;
    }

    private static DefaultMutableTreeNode treeNode(CityComponent component) {
        var node = new DefaultMutableTreeNode(component);
        if (component instanceof ZoneGroup zone) {
            for (CityComponent child : zone.children()) {
                node.add(treeNode(child));
            }
        }
        return node;
    }

    private static String zoneType(ZoneGroup zone) {
        return switch (zone.type()) {
            case CITY -> "City";
            case DISTRICT -> "District";
            case STREET -> "Street";
        };
    }

    private static JPanel card() {
        var panel = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        panel.setLayout(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return panel;
    }

    private static JPanel metric(String title, JLabel value) {
        var panel = new RoundedPanel(METRIC_FILL, DIVIDER, 16);
        panel.setLayout(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        panel.add(label(title, 13, Font.PLAIN, NAV_TEXT), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel label(String text, int size, int style, Color color) {
        var label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private static final class ZoneRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final ScreenIcon ZONE_ICON = new ScreenIcon(ScreenIcon.Shape.ZONES, 16);
        private static final ScreenIcon BUILDING_ICON = new ScreenIcon(ScreenIcon.Shape.BUILDING, 16);

        ZoneRenderer() {
            setBackgroundSelectionColor(SOFT_BLUE);
            setBackgroundNonSelectionColor(Color.WHITE);
            setBorderSelectionColor(BLUE);
            setIconTextGap(7);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                       boolean leaf, int row, boolean focus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focus);
            var node = (CityComponent) ((DefaultMutableTreeNode) value).getUserObject();
            boolean building = !(node instanceof ZoneGroup);
            setIcon(building ? BUILDING_ICON : ZONE_ICON);
            setFont(tree.getFont().deriveFont(building ? Font.PLAIN : Font.BOLD));
            setForeground(selected ? BLUE : INK);
            if (building) {
                boolean on = node.poweredBuildingCount() == 1;
                setText(node.name() + (on ? "  ·  On" : "  ·  Off"));
                if (!selected) {
                    setForeground(on ? NAV_TEXT : POWER_OFF);
                }
            } else {
                setText(node.name());
            }
            return this;
        }
    }
}
