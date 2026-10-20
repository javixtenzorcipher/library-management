package com.folio.library.ui;

import com.folio.library.domain.Book;
import com.folio.library.domain.Loan;
import com.folio.library.domain.Member;
import com.folio.library.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
final class DashboardPanel extends JPanel {
    private final LibraryService service;
    private final Runnable openLoans;
    private final JPanel metrics = new JPanel();
    private final List<JPanel> metricCards = new ArrayList<>();
    private JPanel body;
    private JScrollPane scrollPane;
    private int metricColumns = 4;
    private final DefaultTableModel recentModel = new DefaultTableModel(
            new String[]{"BOOK", "MEMBER", "CHECKED OUT", "DUE DATE", "STATUS"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    DashboardPanel(LibraryService service, Runnable openLoans) {
        this.service = service;
        this.openLoans = openLoans;
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));
        add(createWelcome(), BorderLayout.NORTH);
        body = createBody();
        scrollPane = new JScrollPane(body,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(22);
        scrollPane.getAccessibleContext().setAccessibleName("Dashboard content");
        scrollPane.getAccessibleContext().setAccessibleDescription(
                "Scroll to view library metrics and recent circulation activity");
        add(scrollPane, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateMetricColumns();
            }
        });
        refresh();
    }

    void refresh() {
        updateMetricCards();

        recentModel.setRowCount(0);
        List<Loan> recent = service.getLoans().stream().limit(6).toList();
        for (Loan loan : recent) {
            Book book = service.findBookById(loan.getBookId());
            Member member = service.findMemberById(loan.getMemberId());
            recentModel.addRow(new Object[]{
                    book == null ? "Removed book" : book.getTitle(),
                    member == null ? "Removed member" : member.getFullName(),
                    UiSupport.formatDate(loan.getCheckoutDate()),
                    UiSupport.formatDate(loan.getDueDate()),
                    loan.isActive() ? (loan.isOverdue() ? "Overdue" : "On loan") : "Returned"
            });
        }
        if (recentModel.getRowCount() == 0) {
            recentModel.addRow(new Object[]{"No loans yet", "Start with a checkout", "—", "—", "—"});
        }
    }

    private JPanel createWelcome() {
        JPanel welcome = new JPanel(new BorderLayout(12, 10));
        welcome.setBackground(Theme.TEAL_PALE);
        welcome.setBorder(BorderFactory.createEmptyBorder(19, 23, 19, 22));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("A little more room for great stories.");
        heading.setFont(Theme.font(18, Font.BOLD));
        heading.setForeground(Theme.INK);
        JLabel detail = new JLabel("Your Library, at a glance.");
        detail.setFont(Theme.font(15, Font.PLAIN));
        detail.setForeground(Theme.MUTED);
        text.add(heading);
        text.add(Box.createVerticalStrut(5));
        text.add(detail);
        JButton action = UiSupport.button("  +  New checkout", true);
        action.addActionListener(event -> openLoans.run());
        welcome.add(text, BorderLayout.CENTER);
        welcome.add(action, BorderLayout.EAST);
        return welcome;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(0, 19));
        body.setOpaque(false);
        metrics.setOpaque(false);
        metrics.setPreferredSize(new Dimension(0, metricColumns == 4 ? 112 : 226));
        body.add(metrics, BorderLayout.NORTH);

        JPanel activity = new JPanel(new BorderLayout(0, 12));
        activity.setBackground(Color.WHITE);
        activity.setBorder(UiSupport.panelBorder());
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Recent activity");
        title.setFont(Theme.font(15, Font.BOLD));
        title.setForeground(Theme.INK);
        JButton allLoans = new JButton("View all loans  →");
        allLoans.setFont(Theme.font(12, Font.BOLD));
        allLoans.setForeground(Theme.TEAL);
        allLoans.setContentAreaFilled(false);
        allLoans.setBorderPainted(false);
        allLoans.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        allLoans.addActionListener(event -> openLoans.run());
        heading.add(title, BorderLayout.WEST);
        heading.add(allLoans, BorderLayout.EAST);

        JTable recentTable = new JTable(recentModel);
        UiSupport.styleTable(recentTable);
        UiSupport.styleStatusColumn(recentTable, 4);
        JScrollPane activityScroll = new JScrollPane(recentTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        activityScroll.setPreferredSize(new Dimension(0, 176));
        activityScroll.getAccessibleContext().setAccessibleName("Recent loan activity");
        activity.add(heading, BorderLayout.NORTH);
        activity.add(activityScroll, BorderLayout.CENTER);
        body.add(activity, BorderLayout.CENTER);
        return body;
    }

    private JPanel metric(String title, long value, String detail, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                        BorderFactory.createEmptyBorder(13, 15, 13, 15))));
        JLabel label = new JLabel(title);
        label.setFont(Theme.font(11, Font.BOLD));
        label.setForeground(Theme.MUTED);
        JLabel number = new JLabel(Long.toString(value));
        number.setFont(Theme.font(25, Font.BOLD));
        number.setForeground(Theme.INK);
        JLabel caption = new JLabel(detail);
        caption.setFont(Theme.font(12, Font.PLAIN));
        caption.setForeground(Theme.MUTED);
        JPanel center = new JPanel(new BorderLayout(8, 0));
        center.setOpaque(false);
        center.add(number, BorderLayout.CENTER);
        center.add(caption, BorderLayout.SOUTH);
        card.add(label, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private void updateMetricCards() {
        metricCards.clear();
        metricCards.add(metric("BOOK TITLES", service.getBooks().size(), "In your catalog", Theme.SAGE));
        metricCards.add(metric("AVAILABLE COPIES", service.getAvailableCopyCount(), "Ready to borrow", Theme.TEAL));
        metricCards.add(metric("ACTIVE MEMBERS", service.getActiveMemberCount(), "Registered readers", Theme.AMBER));
        long overdue = service.getOverdueLoanCount();
        metricCards.add(metric("BOOKS ON LOAN", service.getActiveLoanCount(),
                overdue == 0 ? "No overdue books" : overdue + " overdue",
                overdue > 0 ? Theme.RED : Theme.NAVY_LIGHT));
        metrics.removeAll();
        metrics.setLayout(new java.awt.GridLayout(metricColumns == 4 ? 1 : 2, metricColumns,
                14, metricColumns == 4 ? 0 : 12));
        metricCards.forEach(metrics::add);
        metrics.setPreferredSize(new Dimension(0, metricColumns == 4 ? 112 : 226));
        body.setPreferredSize(new Dimension(0, metricColumns == 4 ? 380 : 500));
        metrics.revalidate();
        metrics.repaint();
    }

    private void updateMetricColumns() {
        int nextColumns = getWidth() < 740 ? 2 : 4;
        if (nextColumns != metricColumns) {
            metricColumns = nextColumns;
            updateMetricCards();
            revalidate();
        }
    }

}
