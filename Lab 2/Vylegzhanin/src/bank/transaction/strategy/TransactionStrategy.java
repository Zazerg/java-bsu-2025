package bank.transaction.strategy;

import bank.BankSystemException;
import bank.transaction.Transaction;
import java.util.UUID;

public interface TransactionStrategy {
    Transaction process(UUID userId, UUID accountId, double amount, String payload) throws BankSystemException;
}
