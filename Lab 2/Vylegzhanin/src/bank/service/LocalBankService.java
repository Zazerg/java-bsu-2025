package bank.service;

import bank.Account;
import bank.BankSystemException;
import bank.Server;
import bank.transaction.Transaction;
import bank.User;

import java.util.List;
import java.util.UUID;

public class LocalBankService implements BankService {
    private final Server server;

    public LocalBankService() throws BankSystemException {
        this.server = Server.getInstance();
    }

    @Override
    public User createUser(String name) throws BankSystemException {
        return server.createUser(name);
    }

    @Override
    public Account createAccount(UUID userId) throws BankSystemException {
        return server.createAccount(userId);
    }

    @Override
    public List<User> listUsers(String nameFilter, int limit, int offset) throws BankSystemException {
        return server.listUsers(nameFilter, limit, offset);
    }

    @Override
    public List<UUID> listAccountIds(UUID userId) throws BankSystemException {
        return server.listAccountIds(userId);
    }

    @Override
    public List<UUID> listAccountIdsByPrefix(String prefix, int limit) throws BankSystemException {
        return server.listAccountIdsByPrefix(prefix, limit);
    }

    @Override
    public Account getAccount(UUID accountId) throws BankSystemException {
        return server.getAccount(accountId);
    }

    @Override
    public List<Transaction> listTransactions(int limit, int offset) throws BankSystemException {
        return server.listTransactions(limit, offset);
    }

    @Override
    public Transaction deposit(UUID userId, UUID accountId, double amount) throws BankSystemException {
        return server.Deposit(userId, accountId, amount);
    }

    @Override
    public Transaction withdraw(UUID userId, UUID accountId, double amount) throws BankSystemException {
        return server.Withdraw(userId, accountId, amount);
    }

    @Override
    public Transaction freeze(UUID userId, UUID accountId) throws BankSystemException {
        return server.Freeze(userId, accountId);
    }

    @Override
    public Transaction transfer(UUID userId, UUID sourceAccountId, UUID destinationAccountId, double amount) throws BankSystemException {
        return server.Transfer(userId, sourceAccountId, destinationAccountId, amount);
    }
}
