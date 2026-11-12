package com.smartcity360.ui;

import com.smartcity360.policy.Expression;
import com.smartcity360.policy.PolicyContext;
import com.smartcity360.policy.PolicyParser;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Locale;

import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;

final class CityPoliciesPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color FIELD_FILL = new Color(247, 249, 253);
    private static final Color MATCH_FILL = new Color(235, 248, 241);
    private static final Color MATCH_TEXT = new Color(21, 113, 78);
    private static final Color ALERT_FILL = new Color(255, 241, 241);
    private static final Color ALERT_TEXT = new Color(163, 38, 44);
    private static final String NIGHT_NOISE = "noiseLevel > 55 AND (hour >= 22 OR hour < 6)";
    private static final String PARKING_HOURS = "hour >= 8 AND hour < 18";

    private final JTextArea rule = textArea(5, true);
    private final JTextField hour = new JTextField();
    private final JTextField noise = new JTextField();
    private final JLabel resultTitle = label("Ready to evaluate", 21, Font.BOLD, INK);
    private final JTextArea resultDetails = textArea(2, false);
    private final RoundedPanel resultBox = new RoundedPanel(FIELD_FILL, DIVIDER, 16);
    private final JLabel evaluatedHour = label("—", 19, Font.BOLD, INK);
    private final JLabel evaluatedNoise = label("—", 19, Font.BOLD, INK);
    private final JTextArea evaluatedRule = textArea(3, false);

    CityPoliciesPanel() {
        super(new GridLayout(1, 2, 14, 0));
        setName("City Policies");
        setOpaque(false);
        add(editorCard());
        add(resultCard());
        loadExample(NIGHT_NOISE, "23", "68");
    }

    private JPanel editorCard() {
        var card = card();
        card.add(label("Rule editor", 19, Font.BOLD, INK), BorderLayout.NORTH);
        var form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        var position = new GridBagConstraints();
        position.gridx = 0;
        position.weightx = 1;
        position.fill = GridBagConstraints.HORIZONTAL;

        var night = new ActionButton("Night Noise", false);
        night.addActionListener(event -> loadExample(NIGHT_NOISE, "23", "68"));
        var parking = new ActionButton("Parking Hours", false);
        parking.addActionListener(event -> loadExample(PARKING_HOURS, "8", "48"));
        var examples = new JPanel(new GridLayout(1, 2, 8, 0));
        examples.setOpaque(false);
        examples.add(night);
        examples.add(parking);
        addRow(form, position, examples, 16);

        var ruleLabel = label("Rule text", 13, Font.BOLD, INK);
        ruleLabel.setLabelFor(rule);
        addRow(form, position, ruleLabel, 6);
        rule.setName("policy-rule");
        rule.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        rule.setBackground(FIELD_FILL);
        rule.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        var ruleScroll = new JScrollPane(rule);
        ruleScroll.setBorder(BorderFactory.createLineBorder(DIVIDER));
        addRow(form, position, ruleScroll, 16);

        hour.setName("policy-hour");
        noise.setName("policy-noise");
        var inputs = new JPanel(new GridLayout(1, 2, 10, 0));
        inputs.setOpaque(false);
        inputs.add(input("Hour (0–23)", hour));
        inputs.add(input("Noise (dB)", noise));
        addRow(form, position, inputs, 0);

        var formArea = new JPanel(new BorderLayout());
        formArea.setBackground(Color.WHITE);
        formArea.add(form, BorderLayout.NORTH);
        var scroll = new JScrollPane(formArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scroll, BorderLayout.CENTER);

        var evaluate = new ActionButton("Evaluate Rule", true);
        evaluate.addActionListener(event -> evaluate());
        hour.addActionListener(event -> evaluate());
        noise.addActionListener(event -> evaluate());
        card.add(evaluate, BorderLayout.SOUTH);
        return card;
    }

    private JPanel resultCard() {
        var card = card();
        card.add(label("Evaluation result", 19, Font.BOLD, INK), BorderLayout.NORTH);

        resultBox.setLayout(new BorderLayout(0, 8));
        resultBox.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        resultBox.add(resultTitle, BorderLayout.NORTH);
        resultDetails.setText("Choose an example or enter a rule, then evaluate it.");
        resultBox.add(resultDetails, BorderLayout.CENTER);

        var facts = new JPanel(new GridLayout(1, 2, 8, 0));
        facts.setOpaque(false);
        facts.add(metric("Evaluated hour", evaluatedHour));
        facts.add(metric("Evaluated noise", evaluatedNoise));

        var result = new JPanel(new GridBagLayout());
        result.setOpaque(false);
        var position = new GridBagConstraints();
        position.gridx = 0;
        position.weightx = 1;
        position.fill = GridBagConstraints.HORIZONTAL;
        addRow(result, position, resultBox, 12);
        addRow(result, position, facts, 15);
        addRow(result, position, label("Evaluated rule", 13, Font.BOLD, INK), 5);
        evaluatedRule.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        evaluatedRule.setOpaque(true);
        evaluatedRule.setBackground(FIELD_FILL);
        evaluatedRule.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
        var ruleScroll = new JScrollPane(evaluatedRule);
        ruleScroll.setBorder(BorderFactory.createLineBorder(DIVIDER));
        addRow(result, position, ruleScroll, 0);
        var resultArea = new JPanel(new BorderLayout());
        resultArea.setBackground(Color.WHITE);
        resultArea.add(result, BorderLayout.NORTH);
        var scroll = new JScrollPane(resultArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private void loadExample(String text, String hourValue, String noiseValue) {
        rule.setText(text);
        rule.setCaretPosition(0);
        hour.setText(hourValue);
        noise.setText(noiseValue);
    }

    private void evaluate() {
        String source = rule.getText();
        Expression expression;
        try {
            expression = new PolicyParser().parse(source);
        } catch (IllegalArgumentException error) {
            showError("Invalid rule", error.getMessage());
            return;
        }

        PolicyContext context;
        try {
            int time = parseHour();
            double level = parseNoise();
            context = new PolicyContext(level, time);
        } catch (IllegalArgumentException error) {
            showError("Invalid input", error.getMessage());
            return;
        }

        boolean matched = expression.interpret(context);
        String policy = source.strip();
        boolean violation = matched && NIGHT_NOISE.equals(policy);
        resultTitle.setText(matched ? "Rule matched" : "Rule did not match");
        resultTitle.setForeground(violation ? ALERT_TEXT : matched ? MATCH_TEXT : INK);
        resultBox.setBackground(violation ? ALERT_FILL : matched ? MATCH_FILL : FIELD_FILL);
        resultDetails.setText(resultDescription(policy, matched));
        evaluatedHour.setText(String.format(Locale.ROOT, "%02d:00", (int) context.getFact("hour")));
        evaluatedNoise.setText(Double.toString(context.getFact("noiseLevel")) + " dB");
        evaluatedRule.setText(source);
        evaluatedRule.setCaretPosition(0);
    }

    private int parseHour() {
        try {
            return Integer.parseInt(hour.getText().strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter a whole hour from 0 to 23.");
        }
    }

    private double parseNoise() {
        try {
            return Double.parseDouble(noise.getText().strip());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Enter a numeric noise level in dB.");
        }
    }

    private void showError(String title, String details) {
        resultTitle.setText(title);
        resultTitle.setForeground(ALERT_TEXT);
        resultBox.setBackground(ALERT_FILL);
        resultDetails.setText(details);
        evaluatedHour.setText("—");
        evaluatedNoise.setText("—");
        evaluatedRule.setText("");
    }

    private static String resultDescription(String source, boolean matched) {
        return switch (source) {
            case NIGHT_NOISE -> matched ? "Night noise violation detected." : "The night noise limit was not violated.";
            case PARKING_HOURS -> matched ? "Parking is allowed at this hour." : "Parking is outside the allowed hours.";
            default -> matched ? "The rule evaluated to true." : "The rule evaluated to false.";
        };
    }

    private static void addRow(JPanel form, GridBagConstraints position, JComponent child, int gap) {
        position.insets = new Insets(0, 0, gap, 0);
        form.add(child, position);
    }

    private static JPanel input(String title, JTextField field) {
        var panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        var label = label(title, 13, Font.BOLD, INK);
        label.setLabelFor(field);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(FIELD_FILL);
        field.setForeground(INK);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DIVIDER),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel metric(String title, JLabel value) {
        var panel = new RoundedPanel(FIELD_FILL, DIVIDER, 16);
        panel.setLayout(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        panel.add(label(title, 12, Font.PLAIN, NAV_TEXT), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel card() {
        var panel = new RoundedPanel(Color.WHITE, DIVIDER, 24);
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return panel;
    }

    private static JTextArea textArea(int rows, boolean editable) {
        var area = new JTextArea(rows, 16);
        area.setEditable(editable);
        area.setOpaque(editable);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setForeground(INK);
        return area;
    }

    private static JLabel label(String text, int size, int style, Color color) {
        var label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }
}
