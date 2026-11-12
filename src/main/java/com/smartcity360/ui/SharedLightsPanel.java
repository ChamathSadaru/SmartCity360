package com.smartcity360.ui;

import com.smartcity360.lighting.StreetLight;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;

final class SharedLightsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField search = new JTextField(13);

    SharedLightsPanel(String reference, String configuration, String identity, List<StreetLight> lights, ActionButton close) {
        super(new BorderLayout());
        setOpaque(false);
        var content = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        content.setLayout(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        content.setPreferredSize(new Dimension(550, 454));
        add(content);

        var summary = new JPanel(new GridLayout(0, 1, 0, 4));
        summary.setOpaque(false);
        summary.add(label(reference + " · Shared configuration", 21, Font.BOLD, INK));
        summary.add(label(configuration, 15, Font.BOLD, BLUE));
        summary.add(label("Java identity hash: " + identity, 12, Font.PLAIN, NAV_TEXT));
        content.add(summary, BorderLayout.NORTH);

        var model = new LightsTableModel(lights);
        var table = new TableSurface.RoundedRows(model);
        table.setRowSelectionAllowed(false);
        var sorter = new TableRowSorter<>(model);
        for (int column = 0; column < model.getColumnCount(); column++) {
            sorter.setSortable(column, false);
        }
        table.setRowSorter(sorter);

        search.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DIVIDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        var searchLabel = label("Find light by ID", 12, Font.BOLD, INK);
        searchLabel.setLabelFor(search);
        var searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchLabel, BorderLayout.WEST);
        searchRow.add(search, BorderLayout.CENTER);

        var count = label(formatCount(lights.size()) + " lights share this object", 12, Font.PLAIN, NAV_TEXT);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { update(); }
            @Override public void removeUpdate(DocumentEvent event) { update(); }
            @Override public void changedUpdate(DocumentEvent event) { update(); }

            private void update() {
                String query = search.getText().strip();
                sorter.setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(query), 0));
                count.setText(query.isEmpty() ? formatCount(lights.size()) + " lights share this object"
                        : formatCount(table.getRowCount()) + " of " + formatCount(lights.size()) + " lights");
            }
        });

        var lightList = new JPanel(new BorderLayout(0, 8));
        lightList.setOpaque(false);
        lightList.add(searchRow, BorderLayout.NORTH);
        lightList.add(new TableSurface(table), BorderLayout.CENTER);
        lightList.add(count, BorderLayout.SOUTH);
        content.add(lightList, BorderLayout.CENTER);
        content.add(close, BorderLayout.SOUTH);
    }

    JTextField searchField() {
        return search;
    }

    private static String formatCount(int count) {
        return String.format(Locale.ROOT, "%,d", count);
    }

    private static JLabel label(String text, int size, int style, Color color) {
        var label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private static final class LightsTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] HEADERS = {"Light ID", "Location", "Power"};
        private final transient List<StreetLight> lights;

        LightsTableModel(List<StreetLight> lights) {
            this.lights = lights;
        }

        @Override public int getRowCount() { return lights.size(); }
        @Override public int getColumnCount() { return HEADERS.length; }
        @Override public String getColumnName(int column) { return HEADERS[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            var light = lights.get(row);
            return switch (column) {
                case 0 -> light.id();
                case 1 -> light.location();
                case 2 -> light.isOn() ? "ON" : "OFF";
                default -> throw new IndexOutOfBoundsException(column);
            };
        }
    }
}
