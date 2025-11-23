package ui;

import bank.service.BankService;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class AppFrame extends JFrame {
    public AppFrame(BankService service) {
        super("Banking App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        MainPanel mainPanel = new MainPanel(service);
        TransactionsPanel transactionsPanel = new TransactionsPanel(service);
        tabs.addTab("Dashboard", mainPanel);
        tabs.addTab("Transactions", transactionsPanel);
        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (tabs.getSelectedComponent() == transactionsPanel) {
                    transactionsPanel.refresh();
                } else if (tabs.getSelectedComponent() == mainPanel) {
                    mainPanel.refreshUsers();
                }
            }
        });
        setContentPane(tabs);

        setPreferredSize(new Dimension(900, 600));
        pack();
        setLocationRelativeTo(null);
        setResizable(true);
    }
}
