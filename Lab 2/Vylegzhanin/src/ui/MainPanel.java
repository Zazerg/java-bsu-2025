package ui;

import bank.BankSystemException;
import bank.User;
import bank.service.BankService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.List;
import java.util.UUID;

public class MainPanel extends JPanel {
    private final BankService service;
    private final DefaultListModel<User> userListModel = new DefaultListModel<>();
    private final JList<User> userList = new JList<>(userListModel);
    private final JTextArea userDetails = new JTextArea();
    private final JTextField searchField = new JTextField(16);
    private int offset = 0;
    private int pageSize = 20;
    private int lastFetchedCount = 0;
    private JButton prevButton;
    private JButton nextButton;
    private JComboBox<Integer> pageSizeBox;

    public MainPanel(BankService service) {
        super(new BorderLayout(10, 10));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooters(), BorderLayout.SOUTH);

        refreshUsers();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        header.setBackground(new Color(32, 44, 68));

        JLabel title = new JLabel("Banking Workbench");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(10, 0));

        JPanel summaryPanel = buildAccountSummary();

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setVisibleRowCount(10);
        userList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getName() + "  •  " + value.getId());
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            if (isSelected) {
                label.setBackground(new Color(215, 226, 255));
            } else {
                label.setBackground(Color.WHITE);
            }
            return label;
        });
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetails();
            }
        });

        JScrollPane listScroller = new JScrollPane(userList);
        listScroller.setPreferredSize(new Dimension(320, 260));
        listScroller.setBorder(BorderFactory.createTitledBorder("Users"));
        body.add(listScroller, BorderLayout.WEST);

        userDetails.setEditable(false);
        userDetails.setLineWrap(true);
        userDetails.setWrapStyleWord(true);
        userDetails.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        userDetails.setBackground(new Color(248, 250, 252));
        userDetails.setFont(userDetails.getFont().deriveFont(Font.PLAIN, 13f));

        JScrollPane detailScroller = new JScrollPane(userDetails);
        detailScroller.setBorder(BorderFactory.createTitledBorder("Details"));
        body.add(detailScroller, BorderLayout.CENTER);

        body.add(summaryPanel, BorderLayout.EAST);

        return body;
    }

    private JPanel buildAccountSummary() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setPreferredSize(new Dimension(260, 200));
        JTextArea summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        summaryArea.setBackground(new Color(245, 247, 250));
        summaryArea.setFont(summaryArea.getFont().deriveFont(Font.PLAIN, 13f));

        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);

        Runnable refresh = () -> {
            User user = userList.getSelectedValue();
            if (user == null) {
                summaryArea.setText("Select a user to see balances.");
                return;
            }
            try {
                List<UUID> accounts = service.listAccountIds(user.getId());
                if (accounts.isEmpty()) {
                    summaryArea.setText("No accounts for this user.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Total accounts: ").append(accounts.size()).append("\n");
                double total = 0;
                for (UUID accId : accounts) {
                    bank.Account acc = service.getAccount(accId);
                    if (acc != null) {
                        sb.append(accId).append("\n  balance: ").append(acc.getBalance());
                        if (acc.isFrozen()) {
                            sb.append(" (frozen)");
                        }
                        sb.append("\n\n");
                        total += acc.getBalance();
                    }
                }
                sb.append("Combined balance: ").append(total);
                summaryArea.setText(sb.toString());
            } catch (BankSystemException e) {
                summaryArea.setText("Could not load balances.");
            }
        };

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refresh.run();
            }
        });
        summaryArea.putClientProperty("refresh", refresh);
        refresh.run();
        return panel;
    }

    private JPanel buildFooters() {
        JPanel wrapper = new JPanel(new BorderLayout(6, 6));
        wrapper.add(buildNavBar(), BorderLayout.NORTH);
        wrapper.add(buildActionBar(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));

        prevButton = new JButton("Prev");
        prevButton.addActionListener(e -> goPrev());

        nextButton = new JButton("Next");
        nextButton.addActionListener(e -> goNext());

        pageSizeBox = new JComboBox<>(new Integer[]{10, 20, 50, 100});
        pageSizeBox.setSelectedItem(pageSize);
        pageSizeBox.addActionListener(e -> {
            Integer selected = (Integer) pageSizeBox.getSelectedItem();
            if (selected != null && selected != pageSize) {
                pageSize = selected;
                offset = 0;
                refreshUsers();
            }
        });

        JButton search = new JButton("Search");
        search.addActionListener(e -> {
            offset = 0;
            refreshUsers();
        });
        searchField.addActionListener(e -> {
            offset = 0;
            refreshUsers();
        });

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshUsers());

        JButton addUser = new JButton("Add User");
        addUser.addActionListener(e -> openCreateUserDialog());

        JButton addAccount = new JButton("Create Account for Selected");
        addAccount.addActionListener(e -> openCreateAccountDialog());

        nav.add(new JLabel("Filter:"));
        nav.add(searchField);
        nav.add(search);
        nav.add(prevButton);
        nav.add(nextButton);
        nav.add(new JLabel("Page size:"));
        nav.add(pageSizeBox);
        nav.add(refresh);
        return nav;
    }

    private JPanel buildActionBar() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));

        JButton addUser = new JButton("Add User");
        addUser.addActionListener(e -> openCreateUserDialog());

        JButton addAccount = new JButton("Create Account for Selected");
        addAccount.addActionListener(e -> openCreateAccountDialog());

        JButton newTx = new JButton("New Transaction");
        newTx.addActionListener(e -> openCreateTransactionDialog());

        actions.add(addUser);
        actions.add(addAccount);
        actions.add(newTx);
        return actions;
    }

    private void openCreateUserDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        CreateUserDialog dialog = new CreateUserDialog(owner, service, id -> refreshUsers());
        dialog.setVisible(true);
    }

    private void openCreateAccountDialog() {
        User selected = userList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.", "No user selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        CreateAccountDialog dialog = new CreateAccountDialog(owner, service, selected.getId(), this::refreshUsers);
        dialog.setVisible(true);
    }

    private void openCreateTransactionDialog() {
        User selected = userList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.", "No user selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            List<UUID> accounts = service.listAccountIds(selected.getId());
            if (accounts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Selected user has no accounts. Create an account first.", "No accounts", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (BankSystemException e) {
            JOptionPane.showMessageDialog(this, "Could not load accounts for this user.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        CreateTransactionDialog dialog = new CreateTransactionDialog(owner, service, selected, this::refreshUsers);
        dialog.setVisible(true);
    }

    public void refreshUsers() {
        UUID selectedId = null;
        User currentlySelected = userList.getSelectedValue();
        if (currentlySelected != null) {
            selectedId = currentlySelected.getId();
        }

        userListModel.clear();
        try {
            String query = searchField.getText().trim();
            List<User> users = service.listUsers(query, pageSize, offset);
            lastFetchedCount = users.size();
            for (User user : users) {
                userListModel.addElement(user);
            }
            if (!users.isEmpty()) {
                int targetIndex = 0;
                if (selectedId != null) {
                    for (int i = 0; i < userListModel.size(); i++) {
                        if (userListModel.get(i).getId().equals(selectedId)) {
                            targetIndex = i;
                            break;
                        }
                    }
                }
                userList.setSelectedIndex(targetIndex);
                updateDetails();
            } else {
                userDetails.setText(offset == 0 ? "No users yet. Add one to get started." : "No users found for this page.");
            }
        } catch (BankSystemException e) {
            JOptionPane.showMessageDialog(this, "Could not load users.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        updatePagingButtons();
    }

    private void updateDetails() {
        User user = userList.getSelectedValue();
        if (user == null) {
            userDetails.setText("Select a user to see details.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(user.getName()).append("\n");
        sb.append("ID: ").append(user.getId()).append("\n\n");
        try {
            List<UUID> accounts = service.listAccountIds(user.getId());
            sb.append("Accounts: ").append(accounts.size()).append("\n");
            for (UUID acc : accounts) {
                bank.Account accObj = service.getAccount(acc);
                sb.append(" - ").append(acc);
                if (accObj != null) {
                    sb.append(" | balance: ").append(accObj.getBalance());
                    if (accObj.isFrozen()) {
                        sb.append(" (frozen)");
                    }
                }
                sb.append("\n");
            }
        } catch (BankSystemException e) {
            sb.append("Accounts: unable to load.\n");
        }

        userDetails.setText(sb.toString());
    }

    private void goPrev() {
        if (offset == 0) {
            return;
        }
        offset = Math.max(0, offset - pageSize);
        refreshUsers();
    }

    private void goNext() {
        if (lastFetchedCount < pageSize) {
            return;
        }
        offset += pageSize;
        refreshUsers();
    }

    private void updatePagingButtons() {
        boolean hasPrev = offset > 0;
        boolean hasNext = lastFetchedCount == pageSize;
        if (prevButton != null) {
            prevButton.setEnabled(hasPrev);
        }
        if (nextButton != null) {
            nextButton.setEnabled(hasNext);
        }
    }
}
