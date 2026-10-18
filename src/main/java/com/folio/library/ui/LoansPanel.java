package com.folio.library.ui;

import com.folio.library.domain.Book;
import com.folio.library.domain.Loan;
import com.folio.library.domain.Member;
import com.folio.library.service.LibraryException;
import com.folio.library.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

@SuppressWarnings("serial")
final class LoansPanel extends JPanel {
    private final LibraryService service;
    private final Runnable refreshAll;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "BOOK", "MEMBER", "CHECKED OUT", "DUE DATE", "STATUS"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private JButton returnButton;

    LoansPanel(LibraryService service, Runnable refreshAll) {
        this.service = service;
        this.refreshAll = refreshAll;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        add(createToolbar(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        refresh();
    }

    void refresh() {
        model.setRowCount(0);
        for (Loan loan : service.getLoans()) {
            Book book = service.findBookById(loan.getBookId());
            Member member = service.findMemberById(loan.getMemberId());
            String status = !loan.isActive() ? "Returned" : loan.isOverdue() ? "Overdue" : "On loan";
            model.addRow(new Object[]{loan.getId(), book == null ? "Removed book" : book.getTitle(),
                    member == null ? "Removed member" : member.getFullName(),
                    UiSupport.formatDate(loan.getCheckoutDate()), UiSupport.formatDate(loan.getDueDate()), status});
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{null, "No checkouts yet", "Start a new checkout", "—", "—", "—"});
        }
        updateReturnButton();
    }

    void showCheckoutDialog() {
        var availableBooks = service.getBooks().stream().filter(book -> book.getAvailableCopies() > 0).toList();
        var activeMembers = service.getMembers().stream().filter(Member::isActive).toList();
        if (availableBooks.isEmpty()) {
            UiSupport.error(this, "There are no available books to check out.");
            return;
        }
        if (activeMembers.isEmpty()) {
            UiSupport.error(this, "Add or activate a member before creating a checkout.");
            return;
        }
        JComboBox<BookChoice> bookChoices = new JComboBox<>(availableBooks.stream()
                .map(BookChoice::new).toArray(BookChoice[]::new));
        JComboBox<MemberChoice> memberChoices = new JComboBox<>(activeMembers.stream()
                .map(MemberChoice::new).toArray(MemberChoice[]::new));
        UiSupport.styleComponent(bookChoices, "Book to check out");
        UiSupport.styleComponent(memberChoices, "Member borrowing the book");
        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(6, 2, 4, 2));
        form.add(label("Choose a book"));
        form.add(bookChoices);
        form.add(label("Choose a member"));
        form.add(memberChoices);
        JLabel note = new JLabel("Books are due 14 days after checkout.");
        note.setForeground(Theme.MUTED);
        note.setFont(Theme.font(11, Font.PLAIN));
        form.add(note);
        int result = JOptionPane.showConfirmDialog(this, form, "New checkout",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        Book book = ((BookChoice) bookChoices.getSelectedItem()).book();
        Member member = ((MemberChoice) memberChoices.getSelectedItem()).member();
        try {
            service.checkoutBook(book.getId(), member.getId());
            UiSupport.success(this, "Checkout created. The book is due in 14 days.");
            refreshAll.run();
        } catch (LibraryException exception) {
            UiSupport.error(this, exception.getMessage());
        }
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JLabel hint = new JLabel("Check out available books and record returns.");
        hint.setForeground(Theme.MUTED);
        hint.setFont(Theme.font(12, Font.PLAIN));
        toolbar.add(hint, BorderLayout.WEST);
        JButton checkout = UiSupport.button("+  New checkout", true);
        checkout.addActionListener(event -> showCheckoutDialog());
        toolbar.add(checkout, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 13));
        panel.setBackground(Color.WHITE);
        panel.setBorder(UiSupport.panelBorder());
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Checkout history");
        title.setFont(Theme.font(15, Font.BOLD));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel("Select an active loan to mark a book as returned.");
        subtitle.setFont(Theme.font(12, Font.PLAIN));
        subtitle.setForeground(Theme.MUTED);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(subtitle);
        returnButton = UiSupport.button("Mark returned", false);
        returnButton.addActionListener(event -> returnSelected());
        heading.add(labels, BorderLayout.WEST);
        heading.add(returnButton, BorderLayout.EAST);

        UiSupport.styleTable(table);
        table.removeColumn(table.getColumnModel().getColumn(0));
        table.getAccessibleContext().setAccessibleName("Checkout history");
        table.getAccessibleContext().setAccessibleDescription(
                "Books, borrowing members, checkout dates, due dates, and loan status");
        UiSupport.styleStatusColumn(table, 5);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateReturnButton();
            }
        });
        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void returnSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiSupport.error(this, "Select an active loan first.");
            return;
        }
        Object selectedId = model.getValueAt(table.convertRowIndexToModel(row), 0);
        if (selectedId == null) {
            UiSupport.error(this, "There is no active checkout to return.");
            return;
        }
        String loanId = selectedId.toString();
        Loan loan = service.getLoans().stream().filter(candidate -> candidate.getId().equals(loanId))
                .findFirst().orElse(null);
        if (loan == null || !loan.isActive()) {
            UiSupport.error(this, "This loan has already been returned.");
            return;
        }
        try {
            service.returnBook(loanId);
            refreshAll.run();
        } catch (LibraryException exception) {
            UiSupport.error(this, exception.getMessage());
        }
    }

    private void updateReturnButton() {
        if (returnButton == null) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            returnButton.setEnabled(false);
            return;
        }
        Object selectedId = model.getValueAt(table.convertRowIndexToModel(row), 0);
        if (selectedId == null) {
            returnButton.setEnabled(false);
            return;
        }
        String loanId = selectedId.toString();
        returnButton.setEnabled(service.getLoans().stream()
                .anyMatch(loan -> loan.getId().equals(loanId) && loan.isActive()));
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(12, Font.BOLD));
        label.setForeground(Theme.INK);
        return label;
    }

    private record BookChoice(Book book) {
        @Override
        public String toString() {
            return book.getTitle() + "  ·  " + book.getAvailableCopies() + " available";
        }
    }

    private record MemberChoice(Member member) {
        @Override
        public String toString() {
            return member.getFullName() + "  ·  " + member.getEmail();
        }
    }
}
