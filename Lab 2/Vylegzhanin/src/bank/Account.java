package bank;

import java.util.UUID;

public class Account extends IsValid{
    private UUID uuid;
    private double balance;
    private boolean frozen;

    public Account() {
        uuid = UUID.randomUUID();
        balance = 0;
        frozen = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public Account (UUID uuid, double balance) {
        this.uuid = uuid;
        this.balance = balance;
        this.frozen = false;
    }

    public Account(UUID uuid, double balance, boolean frozen) {
        this.uuid = uuid;
        this.balance = balance;
        this.frozen = frozen;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
}
