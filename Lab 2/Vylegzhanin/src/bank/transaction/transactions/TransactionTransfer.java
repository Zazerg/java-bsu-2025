package bank.transaction.transactions;

import bank.ActionResult;
import bank.transaction.*;

import java.util.UUID;

public class TransactionTransfer extends Transaction {
    private double amount;
    private UUID destinationAccount;

    public TransactionTransfer(TransactionConfig config) {
        super(config);
        this.amount = config.getAmount();
        String dest = config.getProperty("destinationAccount");
        try {
            this.destinationAccount = dest == null ? null : UUID.fromString(dest.trim());
        } catch (IllegalArgumentException ex) {
            this.destinationAccount = null;
        }
    }

    public double getAmount() {
        return amount;
    }

    public UUID getDestinationAccount() {
        return destinationAccount;
    }

    @Override
    public ActionResult visit(TransactionVisitor v) {
        return v.visitTransfer(this);
    }

    @Override
    public ActionResult execute() {
        return getState() == State.COMPLETED ? ActionResult.SUCCESS : ActionResult.FAILURE;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.TRANSFER;
    }
}
