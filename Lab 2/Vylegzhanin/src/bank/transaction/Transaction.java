package bank.transaction;

import bank.ActionResult;

import java.security.Timestamp;
import java.util.Date;
import java.util.UUID;

public abstract class Transaction {
    private final UUID uuid;
    private final Date timestamp;
    private final TransactionType transactionType;
    private State state;
    private final UUID userId;
    private final UUID accountId;

    public UUID getUuid() {
        return uuid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public abstract ActionResult execute();
    public Transaction(TransactionConfig config) {
        this.uuid = config.getUuid();
        this.timestamp = config.getTimestamp();
        this.transactionType = config.getTransactionType();
        this.state = config.getState();
        this.userId = config.getUserId();
        this.accountId = config.getAccountId();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public abstract ActionResult visit (TransactionVisitor v);
}
