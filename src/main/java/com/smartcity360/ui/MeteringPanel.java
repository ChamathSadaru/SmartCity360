package com.smartcity360.ui;

import com.smartcity360.stream.MeteringSession;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;

final class MeteringPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(163, 38, 44);
    private static final DateTimeFormatter RECORDED_AT = DateTimeFormatter.ofPattern("dd MMM, HH:mm:ss", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private static final Icon DECORATORS = new DecoratorIcons();

    private final transient MeteringSession session;
    private final MeterDisplay display = new MeterDisplay();
    private final MeteringFlowPanel flow = new MeteringFlowPanel();
    private final DefaultTableModel rows = new DefaultTableModel(
            new Object[] {"Reading ID", "Meter ID", "Recorded at", "Protection"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final TableSurface.RoundedRows table = new TableSurface.RoundedRows(rows);
    private final JTextArea status = new JTextArea(2, 1);

    MeteringPanel() {
        super(new GridBagLayout());
        setOpaque(false);
        setName("Metering");
        table.removeColumn(table.getColumnModel().getColumn(0));

        MeteringSession opened = null;
        String startupError = null;
        try {
            var saved = new MeteringSession(Path.of(System.getProperty("smartcity360.database", "data/smartcity360.db")));
            saved.listReadings().forEach(this::addReading);
            opened = saved;
        } catch (IllegalStateException error) {
            startupError = error.getMessage();
        }
        session = opened;
        display.setReady(session != null);

        var meterCard = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        meterCard.setLayout(new BorderLayout(0, 9));
        meterCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));
        meterCard.add(display, BorderLayout.CENTER);
        meterCard.add(flow, BorderLayout.SOUTH);

        var columns = new GridBagConstraints();
        columns.gridy = 0;
        columns.gridx = 0;
        columns.fill = GridBagConstraints.BOTH;
        columns.weightx = 0.45;
        columns.weighty = 1;
        columns.insets = new Insets(0, 0, 0, 14);
        add(meterCard, columns);
        columns.gridx = 1;
        columns.weightx = 0.55;
        columns.insets = new Insets(0, 0, 0, 0);
        add(readingsCard(), columns);
        if (startupError != null) {
            report(startupError, true);
        }
    }

    private JPanel readingsCard() {
        var card = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        var title = new JLabel("Readings");
        title.setForeground(INK);
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        card.add(title, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(290, 220));
        table.getColumnModel().getColumn(0).setMinWidth(85);
        table.getColumnModel().getColumn(1).setMinWidth(125);
        table.getColumnModel().getColumn(2).setMinWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(94);
        card.add(new TableSurface(table), BorderLayout.CENTER);

        var write = new ActionButton("Write Reading", true);
        write.setEnabled(session != null);
        write.addActionListener(event -> writeReading());
        var clear = new ActionButton("Clear Readings", false);
        clear.setEnabled(session != null && rows.getRowCount() > 0);
        clear.addActionListener(event -> confirmClearReadings());
        rows.addTableModelListener(event -> clear.setEnabled(session != null && rows.getRowCount() > 0));
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectedDisplay();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (session == null || !SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 1) {
                    return;
                }
                if (table.rowAtPoint(event.getPoint()) < 0) {
                    table.clearSelection();
                    report("Select a saved reading to view it.", false);
                } else {
                    readSelected();
                }
            }
        });

        var buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(write);
        buttons.add(clear);

        status.setOpaque(false);
        status.setEditable(false);
        status.setFocusable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        report(rows.getRowCount() == 0 ? "Write a reading to get started." : "Select a saved reading to view it.", false);

        var actions = new JPanel(new BorderLayout(0, 10));
        actions.setOpaque(false);
        actions.add(buttons, BorderLayout.NORTH);
        actions.add(status, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void writeReading() {
        display.playActivity();
        var dialog = new MeteringDialog(this, "Write Reading");
        var id = new JTextField(16);
        var kwh = new JTextField(16);
        var token = new JPasswordField(16);
        dialog.addField("Meter ID", id);
        dialog.addField("Reading (kWh)", kwh);
        dialog.addField("Access token", token);

        var generate = new ActionButton("Generate Sample", false);
        var copy = new ActionButton("Copy Token", false);
        copy.setEnabled(false);
        generate.addActionListener(event -> {
            var sample = session.generateSample();
            id.setText(sample.meterId());
            kwh.setText(sample.kwh());
            token.setText(sample.token());
            copy.setEnabled(true);
            dialog.showMessage("This PC can open the saved row automatically. Copy the token for manual reads.", false);
        });
        copy.addActionListener(event -> {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(readToken(token)), null);
                dialog.showMessage("Token copied. Use it to verify this reading manually.", false);
            } catch (IllegalStateException error) {
                dialog.showMessage("Clipboard is busy. Try copying again.", true);
            }
        });
        var sampleActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sampleActions.setOpaque(false);
        sampleActions.add(generate);
        sampleActions.add(copy);
        dialog.addRow(sampleActions, 0);

        try {
            dialog.open("Save Reading", () -> {
                beginFlow(MeteringFlowPanel.Direction.WRITE);
                try {
                    var saved = session.save(id.getText(), kwh.getText(), readToken(token), flow::record);
                    addReading(saved.row());
                    int last = rows.getRowCount() - 1;
                    table.setRowSelectionInterval(last, last);
                    table.scrollRectToVisible(table.getCellRect(last, 0, true));
                    animateReading(saved.row().id(), saved.reading(), "Encrypted reading saved to SQLite.");
                    display.playActivity();
                    dialog.dispose();
                } catch (IllegalArgumentException | IllegalStateException | SecurityException error) {
                    dialog.showMessage(error.getMessage(), true);
                    flow.finish(error.getMessage(), true);
                    display.playActivity();
                }
            });
        } finally {
            token.setText("");
            session.clearSampleToken();
        }
    }

    private void readSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            return;
        }
        display.playActivity();
        long id = readingId(selected);
        beginFlow(MeteringFlowPanel.Direction.READ);
        try {
            var reading = session.readAutomatically(id, flow::record);
            if (reading.isPresent()) {
                animateReading(id, reading.get(), "Reading restored on the LCD.");
                return;
            }
        } catch (IllegalStateException | SecurityException error) {
            report(error.getMessage(), true);
            flow.finish(error.getMessage(), true);
            return;
        }
        flow.dismiss();
        var dialog = new MeteringDialog(this, "Read Reading");
        var token = new JPasswordField(16);
        dialog.addField("Access token", token);
        dialog.open("Read Reading", () -> {
            beginFlow(MeteringFlowPanel.Direction.READ);
            try {
                var reading = session.read(id, readToken(token), flow::record);
                animateReading(id, reading, "Reading restored on the LCD.");
                display.playActivity();
                dialog.dispose();
            } catch (IllegalStateException | SecurityException error) {
                dialog.showMessage(error.getMessage(), true);
                flow.finish(error.getMessage(), true);
                display.playActivity();
            }
        });
        token.setText("");
    }

    private void confirmClearReadings() {
        var dialog = new MeteringDialog(this, "Clear Readings");
        var prompt = new JTextArea("Delete all saved meter readings from SQLite? This permanently removes the readings.", 3, 29);
        prompt.setEditable(false);
        prompt.setFocusable(false);
        prompt.setOpaque(false);
        prompt.setLineWrap(true);
        prompt.setWrapStyleWord(true);
        prompt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        prompt.setForeground(INK);
        dialog.addRow(prompt, 0);
        dialog.open("Delete All", () -> {
            try {
                session.clearReadings();
                flow.dismiss();
                rows.setRowCount(0);
                display.showPlaceholder("");
                report("Readings cleared.", false);
                dialog.dispose();
            } catch (IllegalStateException error) {
                dialog.showMessage(error.getMessage(), true);
            }
        });
    }

    private void updateSelectedDisplay() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            display.showPlaceholder("");
            report("Select a saved reading to view it.", false);
            return;
        }
        display.showPlaceholder((String) rows.getValueAt(table.convertRowIndexToModel(selected), 1));
        report("Click a row to view its reading.", false);
    }

    private long readingId(int viewRow) {
        return (long) rows.getValueAt(table.convertRowIndexToModel(viewRow), 0);
    }

    private void beginFlow(MeteringFlowPanel.Direction direction) {
        display.cancelProcessing();
        flow.begin(direction);
    }

    private void animateReading(long id, MeteringSession.Reading reading, String summary) {
        display.blinkWhileProcessing(reading.meterId());
        report("Click a row to view its reading.", false);
        flow.finish(summary, false, () -> {
            int selected = table.getSelectedRow();
            if (selected >= 0 && readingId(selected) == id) {
                display.showReading(reading.meterId(), reading.kwh());
            } else {
                display.cancelProcessing();
            }
        });
    }

    private void addReading(MeteringSession.ReadingRow entry) {
        rows.addRow(new Object[] {entry.id(), entry.meterId(), RECORDED_AT.format(entry.recordedAt()), DECORATORS});
    }

    private void report(String message, boolean error) {
        status.setForeground(error ? ERROR : NAV_TEXT);
        status.setText(message);
    }

    private static String readToken(JPasswordField field) {
        char[] chars = field.getPassword();
        try {
            return new String(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    private static final class DecoratorIcons implements Icon {
        private static final int SIZE = 15;
        private static final int GAP = 5;
        private final Icon[] icons = {
            new ScreenIcon(ScreenIcon.Shape.AUTH, SIZE),
            new ScreenIcon(ScreenIcon.Shape.COMPRESS, SIZE),
            new ScreenIcon(ScreenIcon.Shape.ENCRYPT, SIZE)
        };

        @Override public int getIconWidth() { return SIZE * 3 + GAP * 2; }
        @Override public int getIconHeight() { return SIZE; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            for (Icon icon : icons) {
                icon.paintIcon(component, graphics, x, y);
                x += SIZE + GAP;
            }
        }

        @Override
        public String toString() {
            return "Token check · GZIP compression · AES-GCM encryption";
        }
    }
}
