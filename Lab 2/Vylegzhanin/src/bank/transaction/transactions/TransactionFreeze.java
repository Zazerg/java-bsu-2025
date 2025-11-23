package bank.transaction.transactions;

import bank.ActionResult;
import bank.transaction.*;

public class TransactionFreeze extends Transaction {
    @Override
    public ActionResult visit(TransactionVisitor v) {
        return v.visitFreeze(this);
    }

    public TransactionFreeze(TransactionConfig config) {
        super(config);
    }

    @Override
    public ActionResult execute() {
        return getState() == State.COMPLETED ? ActionResult.SUCCESS : ActionResult.FAILURE;
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.FREEZE;
    }
}
