package ui;

import bank.BankSystemException;
import bank.transaction.Transaction;
import bank.service.BankService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.List;

public class TransactionsPanel extends JPanel {
    private final BankService service;
    private final DefaultTableModel model;
    private int offset = 0;
    private final int pageSize = 20;
    private int lastFetched = 0;
    private final JLabel pageLabel = new JLabel();

    public TransactionsPanel(BankService service) {
        super(new BorderLayout(8, 8));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Transactions");
        title.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 2));
        add(title, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"ID", "Type", "State", "User", "Account", "When"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        add(buildControls(), BorderLayout.SOUTH);

        loadPage();
    }

    private JPanel buildControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadPage());

        JButton prev = new JButton("Prev");
        prev.addActionListener(e -> {
            if (offset > 0) {
                offset = Math.max(0, offset - pageSize);
                loadPage();
            }
        });

        JButton next = new JButton("Next");
        next.addActionListener(e -> {
            if (lastFetched == pageSize) {
                offset += pageSize;
                loadPage();
            }
        });

        controls.add(pageLabel);
        controls.add(prev);
        controls.add(next);
        controls.add(refresh);
        return controls;
    }

    private void loadPage() {
        model.setRowCount(0);
        try {
            List<Transaction> txs = service.listTransactions(pageSize, offset);
            lastFetched = txs.size();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Transaction t : txs) {
                model.addRow(new Object[]{
                        t.getUuid(),
                        t.getTransactionType(),
                        t.getState(),
                        t.getUserId(),
                        t.getAccountId(),
                        fmt.format(t.getTimestamp())
                });
            }
            pageLabel.setText("Showing " + offset + " - " + (offset + txs.size()));
        } catch (BankSystemException e) {
            JOptionPane.showMessageDialog(this, "Could not load transactions.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        loadPage();
    }
}
