package bank.transaction;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

public class TransactionConfig {
    private UUID uuid;
    private Date timestamp;
    private TransactionType transactionType;
    private State state;
    private UUID userId;
    private UUID accountId;
    private double amount;
    private String payload;

    public UUID getUuid() {
        return uuid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public State getState() {
        return state;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getPayload() {
        return payload;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionConfig withUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }
    public TransactionConfig withTimestamp(Date timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    public TransactionConfig withTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }
    public TransactionConfig withState(State state) {
        this.state = state;
        return this;
    }
    public TransactionConfig withUserId(UUID userId) {
        this.userId = userId;
        return this;
    }
    public TransactionConfig withAccountId(UUID accountId) {
        this.accountId = accountId;
        return this;
    }
    public TransactionConfig withAmount(double amount) {
        this.amount = amount;
        return this;
    }
    public TransactionConfig withPayload(String payload) {
        this.payload = payload;
        return this;
    }

    public String getProperty(String key) {
        Properties p = new Properties();
        try {
            p.load(new StringReader(this.payload));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return p.getProperty(key);
    }

    public TransactionConfig() {
        this.uuid = UUID.randomUUID();
        this.timestamp = new Date();
        this.transactionType = TransactionType.INVALID;
        this.state = State.INVALID;
        this.userId = new UUID(0, 0);
        this.accountId = new UUID(0, 0);
        this.amount = -1;
        this.payload = "";
    }

    public TransactionConfig(UUID uuid,  Date timestamp, TransactionType transactionType, State state, UUID userId, UUID accountId, double amount, String payload) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.transactionType = transactionType;
        this.state = state;
        this.userId = userId;
        this.accountId = accountId;
        this.amount = amount;
        this.payload = payload;
    }
}
