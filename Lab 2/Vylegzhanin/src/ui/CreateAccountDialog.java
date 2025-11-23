package ui;

import bank.BankSystemException;
import bank.Account;
import bank.service.BankService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.UUID;

public class CreateAccountDialog extends JDialog {
    private final JTextField userIdField = new JTextField(36);
    private final Runnable onCreated;
    private final BankService service;

    public CreateAccountDialog(Window owner, BankService service, UUID presetUserId, Runnable onCreated) {
        super(owner, "Create Account", ModalityType.APPLICATION_MODAL);
        this.onCreated = onCreated;
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(buildForm(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);

        if (presetUserId != null) {
            userIdField.setText(presetUserId.toString());
            userIdField.setEditable(false);
        }
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("User UUID"), BorderLayout.WEST);
        panel.add(userIdField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JButton create = new JButton("Create");
        create.addActionListener(e -> onCreate());
        getRootPane().setDefaultButton(create);

        actions.add(cancel);
        actions.add(create);
        return actions;
    }

    private void onCreate() {
        String userIdText = userIdField.getText().trim();
        if (userIdText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a user UUID.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdText);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "User UUID is not valid.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Account account = service.createAccount(userId);
            JOptionPane.showMessageDialog(this, "Created account with id: " + account.getUuid(), "Success", JOptionPane.INFORMATION_MESSAGE);
            if (onCreated != null) {
                onCreated.run();
            }
            dispose();
        } catch (BankSystemException ex) {
            JOptionPane.showMessageDialog(this, "Failed to create account for that user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
