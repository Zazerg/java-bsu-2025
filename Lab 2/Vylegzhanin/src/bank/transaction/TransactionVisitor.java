package bank.transaction;

import bank.ActionResult;
import bank.transaction.transactions.TransactionDeposit;
import bank.transaction.transactions.TransactionFreeze;
import bank.transaction.transactions.TransactionTransfer;
import bank.transaction.transactions.TransactionWithdraw;

public interface TransactionVisitor {
    public ActionResult visitDeposit(TransactionDeposit t);
    public ActionResult visitWithdraw(TransactionWithdraw t);
    public ActionResult visitFreeze(TransactionFreeze t);
    public ActionResult visitTransfer(TransactionTransfer t);
}
