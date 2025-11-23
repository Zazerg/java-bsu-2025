package bank.transaction;

public interface TransactionObserver {
    void onTransaction(Transaction transaction);
}
