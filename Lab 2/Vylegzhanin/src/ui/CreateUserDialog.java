package ui;

import bank.BankSystemException;
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
import java.util.function.Consumer;
import java.util.UUID;

public class CreateUserDialog extends JDialog {
    private final JTextField nameField = new JTextField(20);
    private final Consumer<UUID> onCreated;
    private final BankService service;

    public CreateUserDialog(Window owner, BankService service, Consumer<UUID> onCreated) {
        super(owner, "Create User", ModalityType.APPLICATION_MODAL);
        this.onCreated = onCreated;
        this.service = service;
        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(buildForm(), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name"), BorderLayout.WEST);
        panel.add(nameField, BorderLayout.CENTER);
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
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            UUID id = service.createUser(name).getId();
            JOptionPane.showMessageDialog(this, "Created user with id: " + id, "Success", JOptionPane.INFORMATION_MESSAGE);
            if (onCreated != null) {
                onCreated.accept(id);
            }
            dispose();
        } catch (BankSystemException ex) {
            JOptionPane.showMessageDialog(this, "Failed to create user.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
