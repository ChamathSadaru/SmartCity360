package com.smartcity360.ui;

import com.smartcity360.lighting.LightConfiguration;
import com.smartcity360.lighting.SampleStreetlights;
import com.smartcity360.lighting.StreetLight;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;

final class StreetlightsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color PANEL_FILL = new Color(247, 249, 253);
    private static final Color ON = new Color(21, 113, 78);

    private final LampTile first = new LampTile();
    private final LampTile second = new LampTile();
    private final JLabel identity = label("Same object reference: —", 13, Font.BOLD, INK);
    private final JLabel shared = label("Generate lights to compare types.", 12, Font.PLAIN, NAV_TEXT);
    private final JLabel lightCount = label("0", 24, Font.BOLD, INK);
    private final JLabel configurationCount = label("0", 24, Font.BOLD, BLUE);
    private final ActionButton compare = new ActionButton("Compare References", false);
    private final DefaultTableModel rows = new DefaultTableModel(new Object[] {"Ref", "Configuration", "Lights"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private transient List<StreetLight> lights = List.of();
    private transient List<SampleStreetlights.ConfigurationUsage> configurations = List.of();

    StreetlightsPanel() {
        super(new GridLayout(1, 2, 14, 0));
        setName("Streetlights");
        setOpaque(false);
        add(previewCard());
        add(configurationsCard());
    }

    private JPanel previewCard() {
        var card = card();
        card.add(label("Streetlight preview", 19, Font.BOLD, INK), BorderLayout.NORTH);
        var previews = new JPanel(new GridLayout(1, 2, 9, 0));
        previews.setOpaque(false);
        previews.add(first);
        previews.add(second);
        card.add(previews, BorderLayout.CENTER);

        compare.setEnabled(false);
        compare.addActionListener(event -> showReferenceComparison());
        var actions = new JPanel(new BorderLayout(0, 10));
        actions.setOpaque(false);
        var comparison = new RoundedPanel(PANEL_FILL, DIVIDER, 14);
        comparison.setLayout(new BorderLayout(0, 4));
        comparison.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        comparison.add(identity, BorderLayout.NORTH);
        comparison.add(shared, BorderLayout.SOUTH);
        actions.add(comparison, BorderLayout.NORTH);
        actions.add(compare, BorderLayout.SOUTH);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel configurationsCard() {
        var card = card();
        var summary = new JPanel(new BorderLayout(0, 12));
        summary.setOpaque(false);
        var metrics = new JPanel(new GridLayout(1, 2, 8, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Streetlights", lightCount));
        metrics.add(metric("Shared types", configurationCount));
        summary.add(metrics, BorderLayout.NORTH);
        summary.add(label("Shared configurations", 19, Font.BOLD, INK), BorderLayout.SOUTH);
        card.add(summary, BorderLayout.NORTH);

        var table = new TableSurface.RoundedRows(rows);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setToolTipText("Select a configuration to preview it. Double-click or press Enter to see all its lights.");
        table.getColumnModel().getColumn(0).setMinWidth(72);
        table.getColumnModel().getColumn(0).setPreferredWidth(72);
        table.getColumnModel().getColumn(1).setMinWidth(158);
        table.getColumnModel().getColumn(1).setPreferredWidth(182);
        table.getColumnModel().getColumn(2).setMinWidth(72);
        table.getColumnModel().getColumn(2).setPreferredWidth(72);
        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                previewConfiguration(table.convertRowIndexToModel(row));
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                if (row >= 0 && event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    showSharedLights(table.convertRowIndexToModel(row));
                }
            }
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openConfiguration");
        table.getActionMap().put("openConfiguration", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    showSharedLights(table.convertRowIndexToModel(row));
                }
            }
        });
        card.add(new TableSurface(table), BorderLayout.CENTER);

        var generate = new ActionButton("Generate Lights", true);
        generate.addActionListener(event -> generateLights(table));
        card.add(generate, BorderLayout.SOUTH);
        return card;
    }

    private void generateLights(TableSurface.RoundedRows table) {
        lights = SampleStreetlights.generate();
        configurations = SampleStreetlights.configurationUsage(lights);
        rows.setRowCount(0);
        for (var usage : configurations) {
            String ref = String.format(Locale.ROOT, "CFG-%02d", rows.getRowCount() + 1);
            rows.addRow(new Object[] {ref, configurationLabel(usage.configuration()), formatCount(usage.lightCount())});
        }
        lightCount.setText(formatCount(lights.size()));
        configurationCount.setText(formatCount(configurations.size()));
        table.setRowSelectionInterval(0, 0);
    }

    private void previewConfiguration(int row) {
        var configuration = configurations.get(row).configuration();
        String reference = (String) rows.getValueAt(row, 0);
        var previewLights = lightsSharing(configuration).limit(2).toList();
        var firstLight = previewLights.get(0);
        var secondLight = previewLights.get(1);
        first.bind(firstLight, reference);
        second.bind(secondLight, reference);
        boolean sameObject = firstLight.configuration() == secondLight.configuration();
        identity.setText("Same object reference: " + (sameObject ? "Yes" : "No"));
        identity.setForeground(sameObject ? ON : INK);
        shared.setText(reference + " · " + configurationLabel(configuration));
        compare.setEnabled(true);
    }

    private Stream<StreetLight> lightsSharing(LightConfiguration configuration) {
        return lights.stream().filter(light -> light.configuration() == configuration);
    }

    private void showSharedLights(int row) {
        var configuration = configurations.get(row).configuration();
        String reference = (String) rows.getValueAt(row, 0);
        var matchingLights = lightsSharing(configuration).toList();
        var dialog = createDialog(reference + " · Shared configuration");
        var close = new ActionButton("Close", true);
        var content = new SharedLightsPanel(reference, configurationLabel(configuration),
                configurationIdentity(configuration), matchingLights, close);
        openDialog(dialog, content, close, content.searchField());
    }

    private void showReferenceComparison() {
        var dialog = createDialog("Configuration references");
        var content = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        content.setLayout(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        content.add(label("Configuration references", 22, Font.BOLD, INK), BorderLayout.NORTH);

        var cards = new JPanel(new GridLayout(1, 2, 10, 0));
        cards.setOpaque(false);
        cards.add(first.referenceDetails());
        cards.add(second.referenceDetails());
        content.add(cards, BorderLayout.CENTER);

        boolean sameObject = first.light.configuration() == second.light.configuration();
        var footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.add(label("Same object reference: " + (sameObject ? "Yes" : "No"),
                14, Font.BOLD, sameObject ? ON : INK), BorderLayout.NORTH);
        footer.add(label("Hex values are Java identity hash codes.", 12, Font.PLAIN, NAV_TEXT), BorderLayout.CENTER);
        var close = new ActionButton("Close", true);
        footer.add(close, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        openDialog(dialog, content, close, close);
    }

    private JDialog createDialog(String title) {
        var dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, JDialog.ModalityType.DOCUMENT_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return dialog;
    }

    private static void openDialog(JDialog dialog, JPanel content, ActionButton close, JComponent initialFocus) {
        close.addActionListener(event -> dialog.dispose());
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(close);
        ModalPresentation.show(dialog, initialFocus);
    }

    private static String configurationLabel(LightConfiguration configuration) {
        return configuration.bulbType() + " " + configuration.watts() + "W · " + configuration.color();
    }

    private static String configurationIdentity(LightConfiguration configuration) {
        return String.format(Locale.ROOT, "0x%08X", System.identityHashCode(configuration));
    }

    private static String formatCount(int count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    private static JPanel card() {
        var panel = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return panel;
    }

    private static JPanel metric(String title, JLabel value) {
        var panel = new RoundedPanel(PANEL_FILL, DIVIDER, 16);
        panel.setLayout(new BorderLayout(0, 3));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        panel.add(label(title, 12, Font.PLAIN, NAV_TEXT), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel label(String text, int size, int style, Color color) {
        var label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private static final class LampTile extends JPanel {
        private static final long serialVersionUID = 1L;
        private final StreetlightPreview preview = new StreetlightPreview();
        private final JLabel name = label("—", 14, Font.BOLD, INK);
        private final JLabel state = label("—", 13, Font.BOLD, NAV_TEXT);
        private final JLabel location = label("—", 12, Font.PLAIN, NAV_TEXT);
        private final JLabel reference = label("—", 12, Font.BOLD, BLUE);
        private final ActionButton toggle;
        private transient StreetLight light;

        LampTile() {
            super(new BorderLayout());
            setOpaque(false);
            var tile = new RoundedPanel(PANEL_FILL, DIVIDER, 16);
            tile.setLayout(new BorderLayout(0, 5));
            tile.setBorder(BorderFactory.createEmptyBorder(9, 6, 9, 6));
            name.setHorizontalAlignment(SwingConstants.CENTER);
            location.setHorizontalAlignment(SwingConstants.CENTER);
            reference.setHorizontalAlignment(SwingConstants.CENTER);
            state.setHorizontalAlignment(SwingConstants.CENTER);
            var heading = new JPanel(new GridLayout(2, 1, 0, 2));
            heading.setOpaque(false);
            heading.add(name);
            heading.add(location);
            tile.add(heading, BorderLayout.NORTH);
            tile.add(preview, BorderLayout.CENTER);
            var footer = new JPanel(new BorderLayout(0, 5));
            footer.setOpaque(false);
            var details = new JPanel(new GridLayout(2, 1, 0, 2));
            details.setOpaque(false);
            details.add(reference);
            details.add(state);
            footer.add(details, BorderLayout.NORTH);
            toggle = new ActionButton("Toggle Power", false);
            toggle.setEnabled(false);
            toggle.addActionListener(event -> togglePower());
            footer.add(toggle, BorderLayout.SOUTH);
            tile.add(footer, BorderLayout.SOUTH);
            add(tile);
        }

        void bind(StreetLight light, String configReference) {
            this.light = light;
            name.setText(light.id());
            location.setText(light.location());
            reference.setText(configReference);
            preview.getAccessibleContext().setAccessibleDescription(light.location() + ", " + configReference);
            toggle.setEnabled(true);
            refreshPower();
        }

        private void togglePower() {
            if (light.isOn()) {
                light.switchOff();
            } else {
                light.switchOn();
            }
            refreshPower();
        }

        private void refreshPower() {
            preview.setLit(light.isOn());
            state.setText(light.isOn() ? "ON" : "OFF");
            state.setForeground(light.isOn() ? ON : NAV_TEXT);
            toggle.setText("Turn " + light.id() + (light.isOn() ? " Off" : " On"));
            preview.getAccessibleContext().setAccessibleName(light.id() + " streetlight, " + state.getText());
        }

        private JPanel referenceDetails() {
            var card = new RoundedPanel(PANEL_FILL, DIVIDER, 14);
            card.setLayout(new BorderLayout(0, 6));
            card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            card.add(label(light.id() + " · " + light.location(), 14, Font.BOLD, INK), BorderLayout.NORTH);
            card.add(label(reference.getText(), 12, Font.BOLD, BLUE), BorderLayout.CENTER);
            var value = new JLabel(configurationIdentity(light.configuration()));
            value.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
            value.setForeground(INK);
            card.add(value, BorderLayout.SOUTH);
            return card;
        }
    }
}
