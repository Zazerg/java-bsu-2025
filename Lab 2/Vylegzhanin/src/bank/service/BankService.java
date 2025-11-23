package bank.service;

import bank.Account;
import bank.BankSystemException;
import bank.transaction.Transaction;
import bank.User;

import java.util.List;
import java.util.UUID;

public interface BankService {
    User createUser(String name) throws BankSystemException;
    Account createAccount(UUID userId) throws BankSystemException;
    List<User> listUsers(String nameFilter, int limit, int offset) throws BankSystemException;
    List<UUID> listAccountIds(UUID userId) throws BankSystemException;
    List<UUID> listAccountIdsByPrefix(String prefix, int limit) throws BankSystemException;
    Account getAccount(UUID accountId) throws BankSystemException;
    List<Transaction> listTransactions(int limit, int offset) throws BankSystemException;
    Transaction deposit(UUID userId, UUID accountId, double amount) throws BankSystemException;
    Transaction withdraw(UUID userId, UUID accountId, double amount) throws BankSystemException;
    Transaction freeze(UUID userId, UUID accountId) throws BankSystemException;
    Transaction transfer(UUID userId, UUID sourceAccountId, UUID destinationAccountId, double amount) throws BankSystemException;
}
