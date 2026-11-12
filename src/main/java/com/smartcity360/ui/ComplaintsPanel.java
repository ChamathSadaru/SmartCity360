package com.smartcity360.ui;

import com.smartcity360.complaint.Complaint;
import com.smartcity360.complaint.ComplaintHandler;
import com.smartcity360.complaint.HandlingResult;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;
import static com.smartcity360.ui.MainWindow.SOFT_BLUE;

final class ComplaintsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(163, 38, 44);
    private static final Color FIELD_FILL = new Color(247, 249, 253);
    private static final List<DemoComplaint> DEMOS = List.of(
            new DemoComplaint("Broken streetlight", "A streetlight on Lake Street stopped working.", 2),
            new DemoComplaint("Water leak", "Water is leaking onto Market Street from a damaged pipe.", 6),
            new DemoComplaint("District power outage", "Several buildings in North District have lost power.", 9));

    private record DemoComplaint(String label, String description, int severity) {}
    private record Submission(Complaint complaint, HandlingResult result) {}

    private final transient List<Submission> submissions = new ArrayList<>();
    private final JTextArea description = textArea(2, true);
    private final transient List<SeverityButton> severityButtons = new ArrayList<>();
    private final JCheckBox managerAvailable = new JCheckBox("City manager available", true);
    private final JLabel levelInfo = label("", 13, Font.BOLD, INK);
    private final JLabel formStatus = label(" ", 12, Font.PLAIN, NAV_TEXT);
    private final JTextArea selectedDescription = textArea(2, false);
    private final JTextArea route = textArea(3, false);
    private final DefaultTableModel rows = new DefaultTableModel(new Object[] {"ID", "Severity", "Assigned to"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final TableSurface.RoundedRows table = new TableSurface.RoundedRows(rows);
    private int severity = 3;

    ComplaintsPanel() {
        super(new GridLayout(1, 2, 14, 0));
        setName("Citizen Complaints");
        setOpaque(false);
        add(formCard());
        add(historyCard());
    }

    private JPanel formCard() {
        var card = card();
        card.add(label("Demo complaints", 19, Font.BOLD, INK), BorderLayout.NORTH);
        var form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        var position = new GridBagConstraints();
        position.gridx = 0;
        position.weightx = 1;
        position.fill = GridBagConstraints.HORIZONTAL;

        var demos = new JPanel(new GridLayout(0, 1, 0, 5));
        demos.setOpaque(false);
        for (DemoComplaint demo : DEMOS) {
            var button = new ActionButton(demo.label(), false);
            button.addActionListener(event -> {
                description.setText(demo.description());
                description.setCaretPosition(0);
                severityButtons.get(demo.severity() - 1).doClick(0);
                formStatus.setText(" ");
            });
            demos.add(button);
        }
        addField(form, position, demos, 9);

        var descriptionLabel = label("Complaint description", 13, Font.BOLD, INK);
        descriptionLabel.setLabelFor(description);
        addField(form, position, descriptionLabel, 6);
        description.setBackground(FIELD_FILL);
        description.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
        addField(form, position, scroll(description), 8);

        var severityLabel = label("Severity (1–10)", 13, Font.BOLD, INK);
        addField(form, position, severityLabel, 5);
        addField(form, position, severityPicker(), 6);

        managerAvailable.setOpaque(false);
        managerAvailable.setFocusPainted(false);
        managerAvailable.setForeground(INK);
        managerAvailable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        var availabilityIcon = new AvailabilityIcon();
        managerAvailable.setIcon(availabilityIcon);
        managerAvailable.setSelectedIcon(availabilityIcon);
        managerAvailable.setIconTextGap(8);
        managerAvailable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        managerAvailable.setToolTipText("Applies to new complaints.");
        managerAvailable.addItemListener(event -> updateLevelInfo());

        var level = new RoundedPanel(FIELD_FILL, DIVIDER, 24);
        level.setLayout(new BorderLayout(0, 4));
        level.setBorder(BorderFactory.createEmptyBorder(7, 14, 6, 14));
        level.add(levelInfo, BorderLayout.NORTH);
        level.add(managerAvailable, BorderLayout.SOUTH);
        addField(form, position, level, 0);
        updateLevelInfo();
        var formArea = new JPanel(new BorderLayout());
        formArea.setOpaque(false);
        formArea.add(form, BorderLayout.NORTH);
        var formScroll = scroll(formArea);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(formScroll, BorderLayout.CENTER);

        var submit = new ActionButton("Submit Complaint", true);
        submit.addActionListener(event -> submit());
        var footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        footer.add(formStatus, BorderLayout.NORTH);
        footer.add(submit, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel severityPicker() {
        var grid = new JPanel(new GridLayout(2, 5, 7, 5));
        grid.setOpaque(false);
        var group = new ButtonGroup();
        for (int value = 1; value <= 10; value++) {
            int selectedLevel = value;
            var button = new SeverityButton(value);
            group.add(button);
            severityButtons.add(button);
            grid.add(button);
            button.addActionListener(event -> {
                severity = selectedLevel;
                updateLevelInfo();
            });
        }
        severityButtons.get(severity - 1).setSelected(true);
        var row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.add(grid);
        return row;
    }

    private void updateLevelInfo() {
        var preview = new Complaint("Preview", "Severity routing preview", severity);
        var handler = ComplaintHandler.cityRoute(managerAvailable.isSelected()).handle(preview).assignedTo();
        levelInfo.setText("Level " + severity + "  ·  " + handler.orElse("Unassigned"));
        levelInfo.setForeground(handler.isPresent() ? INK : ERROR);
    }

    private JPanel historyCard() {
        var card = card();
        card.add(label("Submitted complaints", 19, Font.BOLD, INK), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        var clear = new ActionButton("Clear Selection", false);
        clear.setEnabled(false);
        clear.addActionListener(event -> table.clearSelection());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                clear.setEnabled(table.getSelectedRow() >= 0);
                showSelection();
            }
        });

        selectedDescription.setText("Submit a complaint to see its route.");
        selectedDescription.setForeground(NAV_TEXT);

        var path = new RoundedPanel(FIELD_FILL, DIVIDER, 16);
        path.setLayout(new BorderLayout(0, 8));
        path.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        path.add(label("Escalation path", 15, Font.BOLD, INK), BorderLayout.NORTH);
        var details = new JPanel(new BorderLayout(0, 6));
        details.setOpaque(false);
        details.add(scrollDetails(selectedDescription), BorderLayout.NORTH);
        details.add(scrollDetails(route), BorderLayout.CENTER);
        path.add(details, BorderLayout.CENTER);

        var results = new JPanel(new GridLayout(2, 1, 0, 12));
        results.setOpaque(false);
        results.add(new TableSurface(table));
        results.add(path);
        card.add(results, BorderLayout.CENTER);

        var actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        actions.setOpaque(false);
        actions.add(clear);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void submit() {
        try {
            String id = String.format(Locale.ROOT, "C-%03d", submissions.size() + 1);
            var complaint = new Complaint(id, description.getText().strip(), severity);
            var result = ComplaintHandler.cityRoute(managerAvailable.isSelected()).handle(complaint);
            submissions.add(new Submission(complaint, result));
            rows.addRow(new Object[] {id, complaint.severity(), result.assignedTo().orElse("Unassigned")});
            int row = rows.getRowCount() - 1;
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
            description.setText("");
            formStatus.setForeground(result.assignedTo().isPresent() ? BLUE : ERROR);
            formStatus.setText(result.assignedTo().isPresent() ? id + " submitted." : id + " is unassigned.");
        } catch (IllegalArgumentException error) {
            formStatus.setForeground(ERROR);
            formStatus.setText("Enter a complaint description.");
        }
    }

    private void showSelection() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            selectedDescription.setText("Select a complaint to see its route.");
            route.setText("");
            return;
        }
        var submission = submissions.get(table.convertRowIndexToModel(selected));
        selectedDescription.setText(submission.complaint().description());
        selectedDescription.setCaretPosition(0);
        route.setText(String.join("\n", submission.result().trace()));
        route.setCaretPosition(0);
    }

    private static void addField(JPanel form, GridBagConstraints position, java.awt.Component field, int gap) {
        position.insets = new Insets(0, 0, gap, 0);
        form.add(field, position);
    }

    private static JScrollPane scroll(java.awt.Component content) {
        var pane = new JScrollPane(content);
        pane.setBorder(BorderFactory.createLineBorder(DIVIDER));
        pane.getViewport().setBackground(Color.WHITE);
        return pane;
    }

    private static JScrollPane scrollDetails(JTextArea content) {
        var pane = scroll(content);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(FIELD_FILL);
        return pane;
    }

    private static JPanel card() {
        var panel = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return panel;
    }

    private static JTextArea textArea(int rows, boolean editable) {
        var text = new JTextArea(rows, 16);
        text.setEditable(editable);
        text.setOpaque(editable);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(INK);
        return text;
    }

    private static JLabel label(String text, int size, int style, Color color) {
        var label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private static final class AvailabilityIcon implements Icon {
        private static final int SIZE = 17;

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            boolean selected = ((JCheckBox) component).isSelected();
            var canvas = (Graphics2D) graphics.create();
            canvas.translate(x, y);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(selected ? BLUE : Color.WHITE);
            canvas.fillOval(0, 0, SIZE - 1, SIZE - 1);
            if (selected) {
                canvas.setColor(Color.WHITE);
                canvas.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                canvas.drawPolyline(new int[] {4, 7, 12}, new int[] {8, 11, 5}, 3);
            } else {
                canvas.setColor(NAV_TEXT);
                canvas.drawOval(0, 0, SIZE - 1, SIZE - 1);
            }
            canvas.dispose();
        }
    }

    private static final class SeverityButton extends JToggleButton {
        private static final long serialVersionUID = 1L;

        SeverityButton(int level) {
            super(Integer.toString(level));
            getAccessibleContext().setAccessibleName("Severity " + level);
            setPreferredSize(new Dimension(31, 31));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(INK);
            addItemListener(event -> setForeground(isSelected() ? Color.WHITE : INK));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            var circle = (Graphics2D) graphics.create();
            circle.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 2;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            circle.setColor(isSelected() ? BLUE : getModel().isRollover() ? SOFT_BLUE : FIELD_FILL);
            circle.fillOval(x, y, size, size);
            circle.setColor(isSelected() || getModel().isRollover() ? BLUE : DIVIDER);
            circle.drawOval(x, y, size, size);
            circle.dispose();
            super.paintComponent(graphics);
        }
    }
}
