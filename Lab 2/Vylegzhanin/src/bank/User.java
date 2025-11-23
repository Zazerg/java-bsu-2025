package bank;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class User extends IsValid {
    private UUID id;
    private String name;
    private List<UUID> accounts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UUID> getAccounts() {
        return accounts;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }

    public User(String name) {
        this(UUID.randomUUID(), name, new ArrayList<>());
    }

    public User(UUID id, String name) {
        this(id, name, new ArrayList<>());
    }

    public User(UUID id, String name, List<UUID> accounts) {
        this.id = id;
        this.name = name;
        this.accounts = accounts;
    }

    public void addAccount(Account account) {
        accounts.add(account.getUuid());
    }
}
