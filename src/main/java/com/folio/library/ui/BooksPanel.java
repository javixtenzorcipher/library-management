package com.folio.library.ui;

import com.folio.library.domain.Book;
import com.folio.library.service.LibraryException;
import com.folio.library.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

@SuppressWarnings("serial")
final class BooksPanel extends JPanel {
    private final LibraryService service;
    private final Runnable refreshAll;
    private final UiSupport.SearchField search =
            new UiSupport.SearchField("Search title, author, ISBN, or category");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "TITLE", "AUTHOR", "ISBN", "CATEGORY", "COPIES", "AVAILABLE"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel();

    BooksPanel(LibraryService service, Runnable refreshAll) {
        this.service = service;
        this.refreshAll = refreshAll;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        add(createToolbar(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        refresh();
    }

    void refresh() {
        String query = search.getText();
        List<Book> books = service.searchBooks(query);
        model.setRowCount(0);
        countLabel.setText(books.size() + (books.size() == 1 ? " book" : " books"));
        for (Book book : books) {
            model.addRow(new Object[]{book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                    book.getCategory(), book.getTotalCopies(), book.getAvailableCopies()});
        }
        if (books.isEmpty()) {
            model.addRow(new Object[]{null, query.isBlank() ? "No books in the catalog" : "No matching books",
                    "Try a different search or add a book.", "", "", "", ""});
        }
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setOpaque(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refresh();
            }
        });
        searchBox.add(search, BorderLayout.CENTER);
        searchBox.setMaximumSize(new java.awt.Dimension(450, 44));
        toolbar.add(searchBox, BorderLayout.CENTER);
        JButton add = UiSupport.button("+  Add book", true);
        add.addActionListener(event -> showBookDialog(null));
        toolbar.add(add, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 13));
        panel.setBackground(Color.WHITE);
        panel.setBorder(UiSupport.panelBorder());
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Library catalog");
        title.setFont(Theme.font(15, Font.BOLD));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel("Add, update, search, and manage your collection.");
        subtitle.setFont(Theme.font(12, Font.PLAIN));
        subtitle.setForeground(Theme.MUTED);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(Box.createVerticalStrut(4));
        labels.add(subtitle);
        countLabel.setFont(Theme.font(12, Font.BOLD));
        countLabel.setForeground(Theme.TEAL_DARK);
        countLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        labels.add(countLabel);
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton edit = UiSupport.button("Edit", false);
        JButton delete = UiSupport.button("Delete", false);
        edit.addActionListener(event -> editSelected());
        delete.addActionListener(event -> deleteSelected());
        actions.add(edit);
        actions.add(delete);
        heading.add(labels, BorderLayout.WEST);
        heading.add(actions, BorderLayout.EAST);

        UiSupport.styleTable(table);
        table.removeColumn(table.getColumnModel().getColumn(0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                boolean selected = row >= 0
                        && model.getValueAt(table.convertRowIndexToModel(row), 0) != null;
                edit.setEnabled(selected);
                delete.setEnabled(selected);
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(210);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getAccessibleContext().setAccessibleName("Book catalog");
        table.getAccessibleContext().setAccessibleDescription("Books with author, ISBN, category, and copy counts");
        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void showBookDialog(Book existing) {
        JTextField title = field(existing == null ? "" : existing.getTitle());
        JTextField author = field(existing == null ? "" : existing.getAuthor());
        JTextField isbn = field(existing == null ? "" : existing.getIsbn());
        JTextField category = field(existing == null ? "" : existing.getCategory());
        JTextField copies = field(existing == null ? "1" : Integer.toString(existing.getTotalCopies()));
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 3));
        form.setBorder(BorderFactory.createEmptyBorder(6, 2, 4, 2));
        addField(form, "Title", title);
        addField(form, "Author", author);
        addField(form, "ISBN (10 or 13 digits)", isbn);
        addField(form, "Category", category);
        addField(form, "Total copies", copies);
        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add a book" : "Update book details",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            int copyCount = Integer.parseInt(copies.getText().trim());
            if (existing == null) {
                service.addBook(title.getText(), author.getText(), isbn.getText(), category.getText(), copyCount);
                UiSupport.success(this, "The book was added to your catalog.");
            } else {
                service.updateBook(existing.getId(), title.getText(), author.getText(), isbn.getText(),
                        category.getText(), copyCount);
                UiSupport.success(this, "Book details were updated.");
            }
            refreshAll.run();
        } catch (NumberFormatException exception) {
            UiSupport.error(this, "Copies must be a whole number.");
        } catch (LibraryException exception) {
            UiSupport.error(this, exception.getMessage());
        }
    }

    private void editSelected() {
        Book book = selectedBook();
        if (book != null) {
            showBookDialog(book);
        }
    }

    private void deleteSelected() {
        Book book = selectedBook();
        if (book == null || !UiSupport.confirm(this, "Delete \"" + book.getTitle() + "\" from the catalog?")) {
            return;
        }
        try {
            service.deleteBook(book.getId());
            refreshAll.run();
        } catch (LibraryException exception) {
            UiSupport.error(this, exception.getMessage());
        }
    }

    private Book selectedBook() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiSupport.error(this, "Select a book first.");
            return null;
        }
        String id = model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
        return service.findBookById(id);
    }

    private static JTextField field(String value) {
        JTextField field = new JTextField(value, 21);
        field.setFont(Theme.font(13, Font.PLAIN));
        return field;
    }

    private static void addField(JPanel panel, String label, JComponent field) {
        JLabel caption = new JLabel(label);
        caption.setFont(Theme.font(12, Font.BOLD));
        caption.setForeground(Theme.INK);
        caption.setLabelFor(field);
        if (field instanceof JTextField textField) {
            UiSupport.styleTextField(textField, label);
        }
        panel.add(caption);
        panel.add(field);
    }
}
