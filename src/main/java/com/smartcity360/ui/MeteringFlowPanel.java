package com.smartcity360.ui;

import com.smartcity360.stream.MeteringEvent;
import com.smartcity360.stream.MeteringEvent.Step;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayDeque;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;
import static com.smartcity360.ui.MainWindow.SOFT_BLUE;

final class MeteringFlowPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color IDLE = new Color(247, 249, 252);
    private static final Color FAILED = new Color(255, 233, 233);
    private static final Color ERROR_TEXT = new Color(163, 38, 44);
    private static final String IDLE_CARD = "idle";
    private static final String FLOW_CARD = "flow";

    enum Direction { WRITE, READ }

    private final StepPill[] pills = {new StepPill(), new StepPill(), new StepPill()};
    private final JLabel direction = label("", 12, Font.BOLD, BLUE);
    private final JTextArea summary = new JTextArea(2, 1);
    private final transient ArrayDeque<MeteringEvent> pending = new ArrayDeque<>();
    private final Timer timer = new Timer(650, event -> revealNext());
    private final Timer hideTimer = new Timer(2400, event -> dismiss());
    private String completionMessage = "";
    private boolean completionError;
    private transient Runnable onComplete;

    MeteringFlowPanel() {
        super(new CardLayout());
        setOpaque(false);

        var strip = new RoundedPanel(Color.WHITE, DIVIDER, 16);
        strip.setLayout(new BorderLayout(0, 7));
        strip.setBorder(BorderFactory.createEmptyBorder(9, 8, 6, 8));
        var header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(label("Decorator Flow", 13, Font.BOLD, INK), BorderLayout.WEST);
        header.add(direction, BorderLayout.EAST);
        strip.add(header, BorderLayout.NORTH);

        var sequence = new JPanel(new GridBagLayout());
        sequence.setOpaque(false);
        var position = new GridBagConstraints();
        position.gridy = 0;
        position.fill = GridBagConstraints.HORIZONTAL;
        for (int index = 0; index < pills.length; index++) {
            position.gridx = index * 2;
            position.weightx = 1;
            sequence.add(pills[index], position);
            if (index + 1 < pills.length) {
                var arrow = label("›", 15, Font.BOLD, NAV_TEXT);
                arrow.setHorizontalAlignment(SwingConstants.CENTER);
                arrow.setPreferredSize(new Dimension(11, 20));
                position.gridx++;
                position.weightx = 0;
                sequence.add(arrow, position);
            }
        }
        strip.add(sequence, BorderLayout.CENTER);

        summary.setOpaque(false);
        summary.setEditable(false);
        summary.setFocusable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        summary.setPreferredSize(new Dimension(1, summary.getPreferredSize().height));
        strip.add(summary, BorderLayout.SOUTH);

        var idle = new JPanel();
        idle.setOpaque(false);
        add(idle, IDLE_CARD);
        add(strip, FLOW_CARD);

        hideTimer.setRepeats(false);
    }

    void begin(Direction operation) {
        dismiss();
        boolean write = operation == Direction.WRITE;
        direction.setText(write ? "Write" : "Read");
        pills[0].reset(Step.TOKEN_CHECK, "Token", "check");
        pills[1].reset(write ? Step.COMPRESS : Step.DECRYPT, write ? "GZIP" : "AES-GCM",
                write ? "compress" : "decrypt");
        pills[2].reset(write ? Step.ENCRYPT : Step.DECOMPRESS, write ? "AES-GCM" : "GZIP",
                write ? "encrypt" : "decompress");
        summary.setText("");
        ((CardLayout) getLayout()).show(this, FLOW_CARD);
    }

    void record(MeteringEvent event) {
        pending.addLast(event);
    }

    void finish(String message, boolean error) {
        finish(message, error, null);
    }

    void finish(String message, boolean error, Runnable completion) {
        completionMessage = message;
        completionError = error;
        onComplete = completion;
        revealNext();
        if (!pending.isEmpty()) {
            timer.start();
        }
    }

    void dismiss() {
        timer.stop();
        hideTimer.stop();
        pending.clear();
        onComplete = null;
        ((CardLayout) getLayout()).show(this, IDLE_CARD);
    }

    private void revealNext() {
        var event = pending.pollFirst();
        if (event != null) {
            for (StepPill pill : pills) {
                if (pill.step == event.step()) {
                    pill.showResult(event.succeeded());
                    break;
                }
            }
        }
        if (pending.isEmpty()) {
            timer.stop();
            summary.setForeground(completionError ? ERROR_TEXT : NAV_TEXT);
            summary.setText(completionMessage);
            var completion = onComplete;
            onComplete = null;
            if (completion != null) {
                completion.run();
            }
            hideTimer.restart();
        }
    }

    private static JLabel label(String text, int size, int style, Color color) {
        var label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private static final class StepPill extends JPanel {
        private static final long serialVersionUID = 1L;
        private final RoundedPanel pill = new RoundedPanel(IDLE, DIVIDER, 42);
        private final JLabel title = label("", 11, Font.BOLD, INK);
        private final JLabel action = label("", 10, Font.PLAIN, NAV_TEXT);
        private Step step;

        StepPill() {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(68, 41));
            setMinimumSize(new Dimension(52, 41));
            pill.setLayout(new GridLayout(2, 1));
            pill.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
            title.setHorizontalAlignment(SwingConstants.CENTER);
            action.setHorizontalAlignment(SwingConstants.CENTER);
            pill.add(title);
            pill.add(action);
            add(pill);
        }

        void reset(Step nextStep, String technology, String operation) {
            step = nextStep;
            title.setText(technology);
            action.setText(operation);
            title.setForeground(INK);
            action.setForeground(NAV_TEXT);
            pill.setBackground(IDLE);
            setStatus("waiting");
        }

        void showResult(boolean succeeded) {
            var color = succeeded ? BLUE : ERROR_TEXT;
            pill.setBackground(succeeded ? SOFT_BLUE : FAILED);
            title.setForeground(color);
            action.setForeground(color);
            setStatus(succeeded ? "completed" : "failed");
        }

        private void setStatus(String status) {
            getAccessibleContext().setAccessibleName(title.getText() + " " + action.getText() + ": " + status);
        }
    }
}
