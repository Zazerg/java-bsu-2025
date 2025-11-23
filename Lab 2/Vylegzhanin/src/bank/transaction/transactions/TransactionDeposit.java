package bank.transaction.transactions;

import bank.ActionResult;
import bank.transaction.*;

import java.util.UUID;

public class TransactionDeposit extends Transaction {
    private double amount;

    public TransactionDeposit(TransactionConfig config) {
        super(config);
        amount = config.getAmount();
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.DEPOSIT;
    }

    @Override
    public ActionResult visit(TransactionVisitor v) {
        return v.visitDeposit(this);
    }

    @Override
    public ActionResult execute() {
        return getState() == State.COMPLETED ? ActionResult.SUCCESS : ActionResult.FAILURE;
    }
}
