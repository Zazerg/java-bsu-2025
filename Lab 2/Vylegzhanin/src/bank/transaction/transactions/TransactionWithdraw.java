package bank.transaction.transactions;

import bank.ActionResult;
import bank.transaction.*;

public class TransactionWithdraw extends Transaction {
    private double amount;

    public TransactionWithdraw(TransactionConfig config) {
        super(config);
        amount = config.getAmount();
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public ActionResult visit(TransactionVisitor v) {
        return v.visitWithdraw(this);
    }

    @Override
    public ActionResult execute() {
        return getState() == State.COMPLETED ? ActionResult.SUCCESS : ActionResult.FAILURE;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.WITHDRAW;
    }
}
