package ui;

import bank.BankSystemException;
import bank.service.LocalBankService;
import javax.swing.SwingUtilities;

public class App {
    public void start() {
        SwingUtilities.invokeLater(() -> {
            try {
                AppFrame frame = new AppFrame(new LocalBankService());
                frame.setVisible(true);
            } catch (BankSystemException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
