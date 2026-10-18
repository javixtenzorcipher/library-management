package com.folio.library.ui;

import com.folio.library.service.LibraryService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("serial")
public final class LibraryAppFrame extends JFrame {
    private final LibraryService service;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pages = new JPanel(cardLayout);
    private final JLabel pageTitle = new JLabel("Overview");
    private final JLabel pageSubtitle = new JLabel("Your library, thoughtfully organized");
    private final JLabel sectionLabel = new JLabel("LIBRARY MANAGEMENT");
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private String currentPage = "dashboard";
    private final DashboardPanel dashboardPanel;
    private final BooksPanel booksPanel;
    private final MembersPanel membersPanel;
    private final LoansPanel loansPanel;

    public LibraryAppFrame(LibraryService service) {
        super("Library Desk");
        this.service = service;
        dashboardPanel = new DashboardPanel(service, this::startCheckout);
        booksPanel = new BooksPanel(service, this::refreshAll);
        membersPanel = new MembersPanel(service, this::refreshAll);
        loansPanel = new LoansPanel(service, this::refreshAll);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 620));
        setSize(1180, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.CANVAS);
        setLayout(new BorderLayout());
        add(createSidebar(), BorderLayout.WEST);
        add(createMainArea(), BorderLayout.CENTER);
        pages.add(dashboardPanel, "dashboard");
        pages.add(booksPanel, "books");
        pages.add(membersPanel, "members");
        pages.add(loansPanel, "loans");
        showPage("dashboard");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.NAVY);
        sidebar.setPreferredSize(new Dimension(194, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(24, 15, 18, 15));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel brand = new JPanel(new BorderLayout(11, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("L", SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(Theme.TEAL);
        mark.setForeground(Color.WHITE);
        mark.setFont(Theme.font(19, Font.BOLD));
        mark.setPreferredSize(new Dimension(38, 38));
        JLabel name = new JLabel("<html><b>LIBRARY</b><br><span style='font-size:10px'>DESK</span></html>");
        name.setForeground(Color.WHITE);
        name.setFont(Theme.font(14, Font.BOLD));
        brand.add(mark, BorderLayout.WEST);
        brand.add(name, BorderLayout.CENTER);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(38));

        JLabel sectionLabel = new JLabel("WORKSPACE");
        sectionLabel.setForeground(new Color(215, 227, 232));
        sectionLabel.setFont(Theme.font(11, Font.BOLD));
        sectionLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 12, 0));
        sectionLabel.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(sectionLabel);
        addNavigation(sidebar, "Overview", "dashboard");
        addNavigation(sidebar, "Books", "books");
        addNavigation(sidebar, "Members", "members");
        addNavigation(sidebar, "Loans", "loans");
        sidebar.add(Box.createVerticalGlue());

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        JLabel avatar = new JLabel("LD", SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(Theme.NAVY_LIGHT);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(Theme.font(11, Font.BOLD));
        avatar.setPreferredSize(new Dimension(34, 34));
        JLabel profile = new JLabel("<html><b>Library staff</b><br><span style='font-size:10px'>Administrator</span></html>");
        profile.setForeground(new Color(222, 229, 236));
        profile.setFont(Theme.font(12, Font.PLAIN));
        footer.add(avatar, BorderLayout.WEST);
        footer.add(profile, BorderLayout.CENTER);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        sidebar.add(footer);
        return sidebar;
    }

    private void addNavigation(JPanel sidebar, String label, String page) {
        JButton button = new JButton(label);
        button.setUI(new BasicButtonUI());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setFont(Theme.font(14, Font.PLAIN));
        button.setForeground(new Color(221, 230, 235));
        button.setBackground(Theme.NAVY);
        button.setBorder(BorderFactory.createEmptyBorder(12, 11, 12, 11));
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                setNavigationBorder(button, isSelectedNavigation(page));
            }

            @Override
            public void focusLost(FocusEvent event) {
                setNavigationBorder(button, isSelectedNavigation(page));
            }
        });
        button.getAccessibleContext().setAccessibleName(label);
        button.getAccessibleContext().setAccessibleDescription("Open the " + label + " workspace");
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.addActionListener(event -> showPage(page));
        navigation.put(page, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5));
    }

    private JPanel createMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.CANVAS);
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 26, 16, 26)));
        pageTitle.setFont(Theme.font(21, Font.BOLD));
        pageTitle.setForeground(Theme.INK);
        pageSubtitle.setFont(Theme.font(12, Font.PLAIN));
        pageSubtitle.setForeground(Theme.MUTED);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(pageTitle);
        labels.add(Box.createVerticalStrut(3));
        labels.add(pageSubtitle);
        sectionLabel.setForeground(Theme.MUTED);
        sectionLabel.setFont(Theme.font(11, Font.BOLD));
        topbar.add(labels, BorderLayout.WEST);
        topbar.add(sectionLabel, BorderLayout.EAST);
        main.add(topbar, BorderLayout.NORTH);
        pages.setBackground(Theme.CANVAS);
        pages.setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 24));
        main.add(pages, BorderLayout.CENTER);
        return main;
    }

    void showPage(String page) {
        currentPage = page;
        String title = switch (page) {
            case "books" -> "Book catalog";
            case "members" -> "Members";
            case "loans" -> "Loans";
            default -> "Overview";
        };
        pageTitle.setText(title);
        pageSubtitle.setText("Your library, thoughtfully organized");
        sectionLabel.setText("LIBRARY MANAGEMENT");
        navigation.forEach((key, button) -> {
            boolean selected = key.equals(page);
            button.setBackground(selected ? Theme.NAVY_LIGHT : Theme.NAVY);
            button.setForeground(selected ? Color.WHITE : new Color(205, 215, 225));
            setNavigationBorder(button, selected);
            button.getAccessibleContext().setAccessibleDescription(
                    selected ? labelFor(key) + " page, current page" : "Open the " + labelFor(key) + " page");
        });
        cardLayout.show(pages, page);
        refreshAll();
    }

    private boolean isSelectedNavigation(String page) {
        return currentPage.equals(page);
    }

    private void setNavigationBorder(JButton button, boolean selected) {
        Border content = selected
                ? BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.TEAL),
                BorderFactory.createEmptyBorder(12, 8, 12, 11))
                : BorderFactory.createEmptyBorder(12, 11, 12, 11);
        button.setBorder(button.hasFocus()
                ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), content)
                : content);
    }

    private void refreshAll() {
        dashboardPanel.refresh();
        booksPanel.refresh();
        membersPanel.refresh();
        loansPanel.refresh();
    }

    private void startCheckout() {
        showPage("loans");
        loansPanel.showCheckoutDialog();
    }

    private static String labelFor(String page) {
        return switch (page) {
            case "books" -> "Books";
            case "members" -> "Members";
            case "loans" -> "Loans";
            default -> "Overview";
        };
    }
}
