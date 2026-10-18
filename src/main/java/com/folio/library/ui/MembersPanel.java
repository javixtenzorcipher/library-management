package com.folio.library.ui;

import com.folio.library.domain.Member;
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

@SuppressWarnings("serial")
final class MembersPanel extends JPanel {
    private final LibraryService service;
    private final Runnable refreshAll;
    private final UiSupport.SearchField search = new UiSupport.SearchField("Search name, email, or member ID");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "NAME", "EMAIL", "PHONE", "STATUS"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel countLabel = new JLabel();
    private JButton editButton;
    private JButton statusButton;
    private JButton deleteButton;

    MembersPanel(LibraryService service, Runnable refreshAll) {
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
        var members = service.searchMembers(search.getText());
        countLabel.setText(members.size() + (members.size() == 1 ? " member" : " members"));
        for (Member member : members) {
            model.addRow(new Object[]{member.getId(), member.getFullName(), member.getEmail(),
                    member.getPhone().isBlank() ? "—" : member.getPhone(),
                    member.isActive() ? "Active" : "Inactive"});
        }
        if (members.isEmpty()) {
            model.addRow(new Object[]{null, search.getText().isBlank() ? "No members in the directory"
                    : "No matching members", "Add a member or try a different search.", "", ""});
        }
        updateActionButtons();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
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
        toolbar.add(search, BorderLayout.CENTER);
        JButton add = UiSupport.button("+  Add member", true);
        add.addActionListener(event -> showMemberDialog(null));
        toolbar.add(add, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 13));
        panel.setBackground(Color.WHITE);
        panel.setBorder(UiSupport.panelBorder());
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Reader directory");
        title.setFont(Theme.font(15, Font.BOLD));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel("Manage reader details and borrowing access.");
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
        editButton = UiSupport.button("Edit", false);
        statusButton = UiSupport.button("Deactivate", false);
        deleteButton = UiSupport.button("Delete", false);
        editButton.addActionListener(event -> editSelected());
        statusButton.addActionListener(event -> toggleStatus());
        deleteButton.addActionListener(event -> deleteSelected());
        actions.add(editButton);
        actions.add(statusButton);
        actions.add(deleteButton);
        heading.add(labels, BorderLayout.WEST);
        heading.add(actions, BorderLayout.EAST);

        UiSupport.styleTable(table);
        table.removeColumn(table.getColumnModel().getColumn(0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getAccessibleContext().setAccessibleName("Member directory");
        table.getAccessibleContext().setAccessibleDescription("Members with contact information and account status");
        UiSupport.styleStatusColumn(table, 4);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void showMemberDialog(Member existing) {
        JTextField name = field(existing == null ? "" : existing.getFullName());
        JTextField email = field(existing == null ? "" : existing.getEmail());
        JTextField phone = field(existing == null ? "" : existing.getPhone());
        JPanel form = new JPanel(new GridLayout(0, 1, 5, 3));
        form.setBorder(BorderFactory.createEmptyBorder(6, 2, 4, 2));
        addField(form, "Full name", name);
        addField(form, "Email", email);
        addField(form, "Phone (optional)", phone);
        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Add a member" : "Update member details",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            if (existing == null) {
                service.addMember(name.getText(), email.getText(), phone.getText());
                UiSupport.success(this, "The member was added.");
            } else {
                service.updateMember(existing.getId(), name.getText(), email.getText(), phone.getText());
                UiSupport.success(this, "Member details were updated.");
            }
            refreshAll.run();
        } catch (LibraryException exception) {
            UiSupport.error(this, exception.getMessage());
        }
    }

    private void editSelected() {
        Member member = selectedMember();
        if (member != null) {
            showMemberDialog(member);
        }
    }

    private void toggleStatus() {
        Member member = selectedMember();
        if (member == null) {
            return;
        }
        boolean activate = !member.isActive();
        try {
            service.setMemberActive(member.getId(), activate);
            refreshAll.run();
        } catch (LibraryException exception) {
            UiSupport.error(this, exception.getMessage());
        }
    }

    private void deleteSelected() {
        Member member = selectedMember();
        if (member == null || !UiSupport.confirm(this,
                "Remove " + member.getFullName()
                        + " from the directory? Past checkout history will be kept.")) {
            return;
        }
        try {
            service.deleteMember(member.getId());
            refreshAll.run();
        } catch (LibraryException exception) {
            UiSupport.error(this, exception.getMessage());
        }
    }

    private Member selectedMember() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiSupport.error(this, "Select a member first.");
            return null;
        }
        String id = model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
        return service.findMemberById(id);
    }

    private void updateActionButtons() {
        Member member = table.getSelectedRow() < 0 ? null : selectedMemberWithoutMessage();
        boolean selected = member != null;
        editButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
        statusButton.setEnabled(selected);
        String statusAction = member == null || member.isActive() ? "Deactivate" : "Activate";
        statusButton.setText(statusAction);
        statusButton.setToolTipText(statusAction + " member access");
        statusButton.getAccessibleContext().setAccessibleName(statusAction + " member access");
    }

    private Member selectedMemberWithoutMessage() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String id = model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
        return service.findMemberById(id);
    }

    private static JTextField field(String value) {
        JTextField field = new JTextField(value, 21);
        field.setFont(Theme.font(15, Font.PLAIN));
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
