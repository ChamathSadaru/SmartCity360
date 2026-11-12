package com.smartcity360.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;

final class MeteringDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Color ERROR = new Color(163, 38, 44);
    private final JPanel form = new JPanel(new GridBagLayout());
    private final JTextArea message = new JTextArea(2, 30);
    private int row;
    private JTextField firstField;

    MeteringDialog(Component parent, String title) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.DOCUMENT_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(event -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        var content = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        content.setLayout(new BorderLayout(0, 18));
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));
        setContentPane(content);
        setMinimumSize(new Dimension(416, 0));

        var heading = new JLabel(title);
        heading.setForeground(INK);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        content.add(heading, BorderLayout.NORTH);
        form.setOpaque(false);
        content.add(form, BorderLayout.CENTER);

        message.setEditable(false);
        message.setFocusable(false);
        message.setOpaque(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    void addField(String text, JTextField field) {
        if (firstField == null) {
            firstField = field;
        }
        var label = new JLabel(text);
        label.setLabelFor(field);
        label.setForeground(INK);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addRow(label, 5);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(INK);
        field.setBackground(new Color(248, 250, 255));
        field.setBorder(fieldBorder(DIVIDER));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { field.setBorder(fieldBorder(BLUE)); }
            @Override public void focusLost(FocusEvent event) { field.setBorder(fieldBorder(DIVIDER)); }
        });
        addRow(field, 11);
    }

    void addRow(JComponent component, int gap) {
        var constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, gap, 0);
        form.add(component, constraints);
    }

    void showMessage(String text, boolean error) {
        message.setForeground(error ? ERROR : NAV_TEXT);
        message.setText(text);
    }

    void open(String actionLabel, Runnable submit) {
        var cancel = new ActionButton("Cancel", false);
        cancel.addActionListener(event -> dispose());
        var action = new ActionButton(actionLabel, true);
        action.addActionListener(event -> submit.run());
        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(action);

        var footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(message, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);
        getContentPane().add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(firstField == null ? cancel : action);
        ModalPresentation.show(this, firstField == null ? cancel : firstField);
    }

    private static javax.swing.border.Border fieldBorder(Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10));
    }
}
