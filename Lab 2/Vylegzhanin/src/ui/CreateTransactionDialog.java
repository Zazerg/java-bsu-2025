package ui;

import bank.BankSystemException;
import bank.transaction.Transaction;
import bank.User;
import bank.service.BankService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.UUID;

/**
 * Dialog to create a transaction for a given user/account.
 */
public class CreateTransactionDialog extends JDialog {
    private final User user;
    private final Runnable onCreated;
    private final BankService service;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private String currentStep = STEP_TYPE;
    private final JComboBox<UUID> accountBox = new JComboBox<>();
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"DEPOSIT", "WITHDRAW", "FREEZE", "TRANSFER"});
    private final JTextField amountField = new JTextField(12);
    private final JTextField destinationField = new JTextField(36);
    private final JPopupMenu suggestionPopup = new JPopupMenu();
    private final JList<String> suggestionList = new JList<>(new DefaultListModel<>());
    private JLabel amountLabel;
    private JLabel destinationLabel;
    private JButton createButton;
    private JButton nextButton;
    private JButton backButton;
    private static final String STEP_TYPE = "stepType";
    private static final String STEP_DETAILS = "stepDetails";

    public CreateTransactionDialog(Window owner, BankService service, User user, Runnable onCreated) {
        super(owner, "New Transaction", ModalityType.APPLICATION_MODAL);
        this.user = user;
        this.onCreated = onCreated;
        this.service = service;

        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        cards.add(buildTypeStep(), STEP_TYPE);
        cards.add(buildDetailStep(), STEP_DETAILS);
        add(cards, BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        boolean loaded = loadAccounts();
        if (!loaded) {
            dispose();
            return;
        }
        pack();
        setLocationRelativeTo(owner);
        updateFieldVisibility();
        showStep(STEP_TYPE);
    }

    private JPanel buildTypeStep() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("User"), c);
        c.gridx = 1;
        panel.add(new JLabel(user.getName() + " (" + user.getId() + ")"), c);

        row++;
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("Transaction type"), c);
        c.gridx = 1;
        typeBox.addActionListener(e -> updateFieldVisibility());
        panel.add(typeBox, c);

        return panel;
    }

    private JPanel buildDetailStep() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("User"), c);
        c.gridx = 1;
        panel.add(new JLabel(user.getName() + " (" + user.getId() + ")"), c);

        row++;
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel("Account"), c);
        c.gridx = 1;
        panel.add(accountBox, c);

        row++;
        c.gridx = 0; c.gridy = row;
        amountLabel = new JLabel("Amount");
        panel.add(amountLabel, c);
        c.gridx = 1;
        panel.add(amountField, c);

        row++;
        c.gridx = 0; c.gridy = row;
        destinationLabel = new JLabel("Destination (transfer)");
        panel.add(destinationLabel, c);
        c.gridx = 1;
        destinationField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshSuggestions(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshSuggestions(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshSuggestions(); }
        });
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String val = suggestionList.getSelectedValue();
                    if (val != null) {
                        destinationField.setText(val);
                        suggestionPopup.setVisible(false);
                    }
                }
            }
        });
        suggestionPopup.add(new javax.swing.JScrollPane(suggestionList));
        panel.add(destinationField, c);

        return panel;
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        backButton = new JButton("Back");
        backButton.addActionListener(e -> showStep(STEP_TYPE));

        nextButton = new JButton("Next");
        nextButton.addActionListener(e -> showStep(STEP_DETAILS));

        createButton = new JButton("Create");
        createButton.addActionListener(e -> onCreate());
        getRootPane().setDefaultButton(createButton);

        actions.add(cancel);
        actions.add(backButton);
        actions.add(nextButton);
        actions.add(createButton);
        return actions;
    }

    private boolean loadAccounts() {
        accountBox.removeAllItems();
        destinationField.setText("");
        try {
            List<UUID> userAccounts = service.listAccountIds(user.getId());
            for (UUID acc : userAccounts) {
                accountBox.addItem(acc);
            }
            if (userAccounts.isEmpty()) {
                createButton.setEnabled(false);
                nextButton.setEnabled(false);
                backButton.setEnabled(false);
                JOptionPane.showMessageDialog(this, "User has no accounts. Create one first.", "No accounts", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } catch (BankSystemException e) {
            createButton.setEnabled(false);
            nextButton.setEnabled(false);
            backButton.setEnabled(false);
            JOptionPane.showMessageDialog(this, "Could not load accounts for this user.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void updateFieldVisibility() {
        String type = (String) typeBox.getSelectedItem();
        boolean needsAmount = !"FREEZE".equals(type);
        boolean needsDestination = "TRANSFER".equals(type);
        amountField.setEnabled(needsAmount);
        amountLabel.setVisible(needsAmount);
        amountField.setVisible(needsAmount);
        destinationField.setEnabled(needsDestination);
        destinationLabel.setVisible(needsDestination);
        destinationField.setVisible(needsDestination);
        if (!needsDestination) {
            suggestionPopup.setVisible(false);
            destinationField.setText("");
        } else {
            refreshSuggestions();
        }
        revalidate();
        repaint();
    }

    private void refreshSuggestions() {
        String prefix = destinationField.getText().trim();
        DefaultListModel<String> model = (DefaultListModel<String>) suggestionList.getModel();
        model.clear();
        if (prefix.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }
        try {
            List<UUID> matches = service.listAccountIdsByPrefix(prefix, 10);
            for (UUID id : matches) {
                model.addElement(id.toString());
            }
            if (!model.isEmpty()) {
                suggestionPopup.setFocusable(false);
                suggestionPopup.setPopupSize(destinationField.getWidth(), 150);
                suggestionPopup.show(destinationField, 0, destinationField.getHeight());
            } else {
                suggestionPopup.setVisible(false);
            }
        } catch (BankSystemException e) {
            suggestionPopup.setVisible(false);
        }
    }

    private void onCreate() {
        if (currentStep().equals(STEP_TYPE)) {
            showStep(STEP_DETAILS);
            return;
        }
        UUID accountId = (UUID) accountBox.getSelectedItem();
        if (accountId == null) {
            JOptionPane.showMessageDialog(this, "Select an account.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String type = (String) typeBox.getSelectedItem();
        Double amount = null;
        if (!"FREEZE".equals(type)) {
            String amtText = amountField.getText().trim();
            if (amtText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter an amount.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                amount = Double.parseDouble(amtText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Amount must be a number.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        UUID destination = null;
        if ("TRANSFER".equals(type)) {
            String destText = destinationField.getText().trim();
            if (destText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a destination account UUID.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                destination = UUID.fromString(destText);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Destination must be a valid UUID.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (destination.equals(accountId)) {
                JOptionPane.showMessageDialog(this, "Destination must differ from source.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            Transaction tx = null;
            switch (type) {
                case "DEPOSIT" -> tx = service.deposit(user.getId(), accountId, amount);
                case "WITHDRAW" -> tx = service.withdraw(user.getId(), accountId, amount);
                case "FREEZE" -> tx = service.freeze(user.getId(), accountId);
                case "TRANSFER" -> {
                    bank.Account destAcc = service.getAccount(destination);
                    if (destAcc == null) {
                        JOptionPane.showMessageDialog(this, "Destination account does not exist.", "Validation", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    tx = service.transfer(user.getId(), accountId, destination, amount);
                }
            }
            if (tx == null) {
                JOptionPane.showMessageDialog(this, "Failed to create transaction.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Created transaction: " + tx.getUuid(), "Success", JOptionPane.INFORMATION_MESSAGE);
            if (onCreated != null) {
                onCreated.run();
            }
            dispose();
        } catch (BankSystemException ex) {
            JOptionPane.showMessageDialog(this, "Could not create transaction.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showStep(String step) {
        cardLayout.show(cards, step);
        currentStep = step;
        boolean onDetails = STEP_DETAILS.equals(step);
        backButton.setVisible(onDetails);
        nextButton.setVisible(!onDetails);
        createButton.setVisible(onDetails);
        updateFieldVisibility();
    }

    private String currentStep() {
        return currentStep;
    }
}
