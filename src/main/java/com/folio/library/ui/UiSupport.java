package com.folio.library.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Component;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class UiSupport {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private UiSupport() {
    }

    static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(Theme.font(13, Font.BOLD));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(10, 16, 10, 16));
        int labelWidth = button.getFontMetrics(button.getFont()).stringWidth(text);
        button.setPreferredSize(new java.awt.Dimension(
                Math.max(button.getPreferredSize().width, labelWidth + 38),
                Math.max(44, button.getPreferredSize().height)));
        button.setBackground(primary ? Theme.TEAL : Theme.WHITE);
        button.setForeground(primary ? Theme.WHITE : Theme.INK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? Theme.TEAL : Theme.BORDER),
                BorderFactory.createEmptyBorder(0, 3, 0, 3)));
        button.setRolloverEnabled(true);
        button.addChangeListener(event -> {
            if (!button.isEnabled()) {
                button.setForeground(Theme.MUTED);
                button.setBackground(Theme.CANVAS);
            } else if (button.getModel().isPressed()) {
                button.setForeground(primary ? Theme.WHITE : Theme.INK);
                button.setBackground(primary ? Theme.TEAL_DARK : Theme.HOVER);
            } else if (button.getModel().isRollover()) {
                button.setForeground(primary ? Theme.WHITE : Theme.INK);
                button.setBackground(primary ? Theme.TEAL_DARK : Theme.HOVER);
            } else {
                button.setForeground(primary ? Theme.WHITE : Theme.INK);
                button.setBackground(primary ? Theme.TEAL : Theme.WHITE);
            }
        });
        button.setToolTipText(text.replaceAll("^\\s*[+→]\\s*", ""));
        button.getAccessibleContext().setAccessibleName(button.getToolTipText());
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.FOCUS, 2),
                        BorderFactory.createEmptyBorder(0, 2, 0, 2)));
            }

            @Override
            public void focusLost(FocusEvent event) {
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(primary ? Theme.TEAL : Theme.BORDER),
                        BorderFactory.createEmptyBorder(0, 3, 0, 3)));
            }
        });
        return button;
    }

    static void styleTable(JTable table) {
        int[] hoveredRow = {-1};
        table.setFont(Theme.font(13, Font.PLAIN));
        table.setForeground(Theme.INK);
        table.setBackground(Theme.WHITE);
        table.setRowHeight(48);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.BORDER);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(Theme.TEAL_PALE);
        table.setSelectionForeground(Theme.INK);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.font(11, Font.BOLD));
        header.setForeground(Theme.MUTED);
        header.setBackground(Theme.WHITE);
        header.setPreferredSize(new java.awt.Dimension(header.getPreferredSize().width, 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                                                             boolean focused, int row, int column) {
                Component component = super.getTableCellRendererComponent(source, value, selected, focused, row,
                        column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!selected) {
                    component.setBackground(row == hoveredRow[0] ? Theme.HOVER
                            : row % 2 == 0 ? Theme.WHITE : Theme.ROW_ALT);
                }
                return component;
            }
        };
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                if (hoveredRow[0] != row) {
                    hoveredRow[0] = row;
                    table.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoveredRow[0] = -1;
                table.repaint();
            }
        });
        table.setToolTipText("Select a row to view available actions.");
    }

    static Border panelBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(18, 20, 18, 20));
    }

    static void styleTextField(javax.swing.JTextField field, String accessibleName) {
        field.setFont(Theme.font(15, Font.PLAIN));
        field.setForeground(Theme.INK);
        field.setCaretColor(Theme.TEAL);
        field.setSelectionColor(Theme.TEAL_PALE);
        field.setSelectedTextColor(Theme.INK);
        field.setOpaque(true);
        field.setBackground(Theme.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        field.getAccessibleContext().setAccessibleName(accessibleName);
        field.setToolTipText(accessibleName);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.FOCUS, 2),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }

            @Override
            public void focusLost(FocusEvent event) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER, 1),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)));
            }
        });
    }

    static final class SearchField extends javax.swing.JTextField {
        private static final long serialVersionUID = 1L;
        private final String prompt;

        SearchField(String prompt) {
            this.prompt = prompt;
            setColumns(28);
            styleTextField(this, prompt);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D copy = (Graphics2D) graphics.create();
                try {
                    Insets insets = getInsets();
                    FontMetrics metrics = copy.getFontMetrics(getFont());
                    copy.setColor(Theme.MUTED);
                    copy.drawString(prompt, insets.left,
                            (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                } finally {
                    copy.dispose();
                }
            }
        }
    }

    static void styleComponent(JComponent component, String accessibleName) {
        component.getAccessibleContext().setAccessibleName(accessibleName);
        component.setFont(Theme.font(14, Font.PLAIN));
        component.setBackground(Theme.WHITE);
        component.setForeground(Theme.INK);
    }

    static void styleStatusColumn(JTable table, int modelColumn) {
        int viewColumn = table.convertColumnIndexToView(modelColumn);
        if (viewColumn < 0) {
            return;
        }
        table.getColumnModel().getColumn(viewColumn).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                                                             boolean focused, int row, int column) {
                Component component = super.getTableCellRendererComponent(source, value, selected, focused, row,
                        column);
                String status = value == null ? "" : value.toString();
                Color foreground = status.equals("Overdue") || status.equals("Inactive")
                        ? Theme.RED : status.equals("On loan") || status.equals("Active")
                        ? Theme.TEAL_DARK : Theme.MUTED;
                setFont(Theme.font(12, Font.BOLD));
                setForeground(selected ? Theme.INK : foreground);
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 7));
                setOpaque(true);
                setBackground(status.equals("Overdue") || status.equals("Inactive") ? Theme.RED_PALE
                        : status.equals("On loan") || status.equals("Active") ? Theme.TEAL_PALE
                        : selected ? Theme.TEAL_PALE
                        : row % 2 == 0 ? Theme.WHITE : Theme.ROW_ALT);
                return component;
            }
        });
    }

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check this action", JOptionPane.WARNING_MESSAGE);
    }

    static void success(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm action",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static String formatDate(LocalDate date) {
        return DATE_FORMAT.format(date);
    }
}
