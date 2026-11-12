package com.smartcity360.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

import static com.smartcity360.ui.MainWindow.BLUE;
import static com.smartcity360.ui.MainWindow.DIVIDER;
import static com.smartcity360.ui.MainWindow.INK;
import static com.smartcity360.ui.MainWindow.NAV_TEXT;
import static com.smartcity360.ui.MainWindow.SOFT_BLUE;

final class TableSurface extends JScrollPane {
    private static final long serialVersionUID = 1L;
    private static final Color HEADER_FILL = new Color(241, 245, 252);
    private static final Color ROW_FILL = new Color(246, 248, 252);
    private static final int ARC = 16;

    TableSurface(RoundedRows table) {
        super(table);
        setBorder(BorderFactory.createEmptyBorder());
        setBackground(Color.WHITE);
        getViewport().setBackground(Color.WHITE);
        setColumnHeaderView(table.getTableHeader());
        getColumnHeader().setBackground(HEADER_FILL);
        var corner = new JPanel();
        corner.setBackground(HEADER_FILL);
        setCorner(UPPER_RIGHT_CORNER, corner);

        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setForeground(INK);
        table.setSelectionBackground(SOFT_BLUE);
        table.setSelectionForeground(INK);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setDefaultRenderer(Object.class, new CellRenderer(false));
        var header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new CellRenderer(true));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
    }

    @Override
    public void paint(Graphics graphics) {
        var outline = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, ARC, ARC);
        var content = (Graphics2D) graphics.create();
        content.clip(outline);
        super.paint(content);
        content.dispose();

        var edge = (Graphics2D) graphics.create();
        edge.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        edge.setColor(DIVIDER);
        edge.draw(outline);
        edge.dispose();
    }

    static final class RoundedRows extends JTable {
        private static final long serialVersionUID = 1L;

        RoundedRows(TableModel model) {
            super(model);
            setOpaque(false);
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent event) { repaint(); }
                @Override public void focusLost(FocusEvent event) { repaint(); }
            });
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getParent() instanceof JViewport viewport) {
                int minimumWidth = 0;
                for (int column = 0; column < getColumnCount(); column++) {
                    minimumWidth += getColumnModel().getColumn(column).getMinWidth();
                }
                if (viewport.getWidth() < minimumWidth) {
                    return false;
                }
            }
            return super.getScrollableTracksViewportWidth();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            var clip = graphics.getClipBounds();
            int first = clip == null ? 0 : rowAtPoint(new Point(0, Math.max(0, clip.y)));
            if (first >= 0 && first < getRowCount()) {
                int last = clip == null ? -1 : rowAtPoint(new Point(0, clip.y + clip.height - 1));
                paintRowBackgrounds(graphics, first, last < 0 ? getRowCount() - 1 : last);
            }
            super.paintComponent(graphics);
        }

        private void paintRowBackgrounds(Graphics graphics, int first, int last) {
            var background = (Graphics2D) graphics.create();
            background.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int focusedRow = getSelectionModel().getLeadSelectionIndex();
            for (int row = first; row <= last; row++) {
                var bounds = getCellRect(row, 0, true);
                int height = bounds.height - 7;
                var pill = new RoundRectangle2D.Double(6.5, bounds.y + 3.5, getWidth() - 13, height, height, height);
                boolean selected = isRowSelected(row);
                background.setColor(selected ? SOFT_BLUE : ROW_FILL);
                background.fill(pill);
                if (selected && hasFocus() && row == focusedRow) {
                    background.setColor(BLUE);
                    background.draw(pill);
                }
            }
            background.dispose();
        }
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final Border HEADER_BORDER = padded(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER), 12);
        private static final Border ROW_PADDING = BorderFactory.createEmptyBorder(0, 15, 0, 12);
        private final boolean header;

        CellRenderer(boolean header) {
            this.header = header;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setIcon(null);
            setToolTipText(null);
            setHorizontalAlignment(SwingConstants.LEADING);
            setOpaque(header);
            if (header) {
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBackground(HEADER_FILL);
                setForeground(NAV_TEXT);
                setBorder(HEADER_BORDER);
            } else {
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                setForeground(selected ? BLUE : INK);
                setBorder(ROW_PADDING);
                if (value instanceof Icon icon) {
                    setText("");
                    setIcon(icon);
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setToolTipText(icon.toString());
                    getAccessibleContext().setAccessibleName(icon.toString());
                } else {
                    getAccessibleContext().setAccessibleName(null);
                }
            }
            return this;
        }

        private static Border padded(Border border, int horizontal) {
            return BorderFactory.createCompoundBorder(border,
                    BorderFactory.createEmptyBorder(0, horizontal, 0, horizontal));
        }
    }
}
