package bank.transaction;

import bank.transaction.transactions.TransactionDeposit;
import bank.transaction.transactions.TransactionFreeze;
import bank.transaction.transactions.TransactionTransfer;
import bank.transaction.transactions.TransactionWithdraw;

import java.util.HashMap;
import java.util.Map;

public class TransactionFactory {
    public static final Map<String, TransactionType>  TRANSACTION_TYPES = new HashMap<>();
    static {
        TRANSACTION_TYPES.put("DEPOSIT", TransactionType.DEPOSIT);
        TRANSACTION_TYPES.put("WITHDRAW", TransactionType.WITHDRAW);
        TRANSACTION_TYPES.put("FREEZE", TransactionType.FREEZE);
        TRANSACTION_TYPES.put("TRANSFER", TransactionType.TRANSFER);
    }

    public static Transaction createTransaction(TransactionType type, TransactionConfig config) {
        Transaction transaction =
        switch (type) {
            case DEPOSIT -> new TransactionDeposit(config);
            case WITHDRAW -> new TransactionWithdraw(config);
            case FREEZE ->  new TransactionFreeze(config);
            case TRANSFER ->  new TransactionTransfer(config);
            case INVALID -> null;
        };
        return transaction;
    }

    public static Transaction createTransaction(String type, TransactionConfig config) {
        return createTransaction(TransactionType.valueOf(type.toUpperCase()), config);
    }
}
