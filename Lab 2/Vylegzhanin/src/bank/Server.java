package bank;

import bank.database.UserRepository;
import bank.transaction.*;
import bank.transaction.strategy.TransactionStrategy;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Server {
    private static volatile Server instance;

    private final UserRepository userRepository;
    private final ConcurrentMap<UUID, Object> accountLocks = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<TransactionObserver> observers = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<TransactionType, TransactionStrategy> strategies = new EnumMap<>(TransactionType.class);
    private final TransactionObserver consoleLogger = tx -> System.out.println("TX " + tx.getTransactionType() + " " + tx.getUuid() + " user=" + tx.getUserId() + " account=" + tx.getAccountId() + " ts=" + Instant.ofEpochMilli(tx.getTimestamp().getTime()) + " state=" + tx.getState());

    public User createUser(String name) throws BankSystemException {
        User user = new User(name);
        try {
            userRepository.addUser(user);
        } catch (SQLException e) {
            throw new BankSystemException();
        }
        return user;
    }

    public Account createAccount(UUID userId) throws BankSystemException {
        return createAccount(getUserByUUID(userId));
    }

    public User getUserByUUID(UUID id) throws BankSystemException {
        try {
            String name = userRepository.findUserNameById(id);
            if (name == null) {
                return null;
            }
            User user = new User(id, name, userRepository.findAccountIdsByUser(id));
            user.makeValid();
            return user;
        }  catch (SQLException e) {
            throw new BankSystemException();
        }
    };

    public Account createAccount(User user) throws BankSystemException {
        Account account = new Account();
        try {
            userRepository.addAccount(account.getUuid(), user.getId(), account.getBalance());
            account.makeValid();
            user.addAccount(account);
        } catch (SQLException e) {
            throw new BankSystemException();
        }
        return account;
    }

    public static Server getInstance() throws BankSystemException {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    public List<User> listUsers(String nameFilter, int limit, int offset) throws BankSystemException {
        try {
            String pattern = nameFilter == null || nameFilter.isEmpty() ? "%" : "%" + nameFilter + "%";
            return userRepository.findUsers(pattern, limit, offset);
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    public List<UUID> listAccountIds(UUID userId) throws BankSystemException {
        try {
            return userRepository.findAccountIdsByUser(userId);
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    public List<UUID> listAccountIdsByPrefix(String prefix, int limit) throws BankSystemException {
        try {
            return userRepository.findAccountIdsByPrefix(prefix, limit);
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    public Account getAccount(UUID accountId) throws BankSystemException {
        try {
            return userRepository.getAccount(accountId);
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    public List<Transaction> listTransactions(int limit, int offset) throws BankSystemException {
        try {
            return userRepository.findTransactions(limit, offset);
        }  catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    public Transaction Deposit(UUID userId, UUID accountId, double amount) throws BankSystemException {
        return withAccountLock(accountId, () -> runStrategy(TransactionType.DEPOSIT, userId, accountId, amount, ""));
    }

    public Transaction Withdraw(UUID userId, UUID accountId, double amount) throws BankSystemException {
        return withAccountLock(accountId, () -> runStrategy(TransactionType.WITHDRAW, userId, accountId, amount, ""));
    }

    public Transaction Freeze(UUID userId, UUID accountId) throws BankSystemException {
        return withAccountLock(accountId, () -> runStrategy(TransactionType.FREEZE, userId, accountId, -1, ""));
    }

    public Transaction Transfer(UUID userId, UUID sourceAccountId, UUID destinationAccountId, double amount) throws BankSystemException {
        return withTwoAccountLocks(sourceAccountId, destinationAccountId, () -> runStrategy(TransactionType.TRANSFER, userId, sourceAccountId, amount, destinationAccountId.toString()));
    }

    public Future<Transaction> submitDeposit(UUID userId, UUID accountId, double amount) {
        return executor.submit(() -> Deposit(userId, accountId, amount));
    }

    public Future<Transaction> submitWithdraw(UUID userId, UUID accountId, double amount) {
        return executor.submit(() -> Withdraw(userId, accountId, amount));
    }

    public Future<Transaction> submitFreeze(UUID userId, UUID accountId) {
        return executor.submit(() -> Freeze(userId, accountId));
    }

    public Future<Transaction> submitTransfer(UUID userId, UUID sourceAccountId, UUID destinationAccountId, double amount) {
        return executor.submit(() -> Transfer(userId, sourceAccountId, destinationAccountId, amount));
    }

    public void addObserver(TransactionObserver observer) {
        observers.addIfAbsent(observer);
    }

    public void removeObserver(TransactionObserver observer) {
        observers.remove(observer);
    }

    private Transaction processDeposit(UUID userId, UUID accountId, double amount) throws BankSystemException {
        if (amount <= 0 || !validateUserAccount(userId, accountId)) {
            return null;
        }
        try {
            Account account = userRepository.getAccount(accountId);
            if (account == null || account.isFrozen()) {
                return null;
            }
            double newBalance = account.getBalance() + amount;
            userRepository.updateAccountBalance(accountId, newBalance);
            Transaction t = buildTransaction(TransactionType.DEPOSIT, userId, accountId, amount, "");
            t.setState(State.COMPLETED);
            userRepository.addTransaction(t);
            notifyObservers(t);
            return t;
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    private Transaction processWithdraw(UUID userId, UUID accountId, double amount) throws BankSystemException {
        if (amount <= 0 || !validateUserAccount(userId, accountId)) {
            return null;
        }
        try {
            Account account = userRepository.getAccount(accountId);
            if (account == null || account.isFrozen()) {
                return null;
            }
            if (account.getBalance() < amount) {
                return null;
            }
            double newBalance = account.getBalance() - amount;
            userRepository.updateAccountBalance(accountId, newBalance);
            Transaction t = buildTransaction(TransactionType.WITHDRAW, userId, accountId, amount, "");
            t.setState(State.COMPLETED);
            userRepository.addTransaction(t);
            notifyObservers(t);
            return t;
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    private Transaction processFreeze(UUID userId, UUID accountId) throws BankSystemException {
        if (!validateUserAccount(userId, accountId)) {
            return null;
        }
        try {
            Account account = userRepository.getAccount(accountId);
            if (account == null) {
                return null;
            }
            userRepository.updateAccountFrozen(accountId, true);
            Transaction t = buildTransaction(TransactionType.FREEZE, userId, accountId, -1, "");
            t.setState(State.COMPLETED);
            userRepository.addTransaction(t);
            notifyObservers(t);
            return t;
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    private Transaction processTransfer(UUID userId, UUID sourceAccountId, UUID destinationAccountId, double amount) throws BankSystemException {
        if (amount <= 0) {
            return null;
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            return null;
        }
        try {
            if (!validateUserAccount(userId, sourceAccountId)) {
                return null;
            }
            Account source = userRepository.getAccount(sourceAccountId);
            Account dest = userRepository.getAccount(destinationAccountId);
            if (source == null || dest == null || source.isFrozen() || dest.isFrozen()) {
                return null;
            }
            if (source.getBalance() < amount) {
                return null;
            }
            userRepository.updateAccountBalance(sourceAccountId, source.getBalance() - amount);
            userRepository.updateAccountBalance(destinationAccountId, dest.getBalance() + amount);
            Transaction t = buildTransaction(TransactionType.TRANSFER, userId, sourceAccountId, amount, "destinationAccount=" + destinationAccountId);
            t.setState(State.COMPLETED);
            userRepository.addTransaction(t);
            notifyObservers(t);
            return t;
        } catch (SQLException e) {
            throw new BankSystemException();
        }
    }

    private Transaction buildTransaction(TransactionType type, UUID userId, UUID accountId, double amount, String payload) {
        TransactionConfig config = new TransactionConfig()
                .withUuid(UUID.randomUUID())
                .withTimestamp(new Date())
                .withTransactionType(type)
                .withState(State.IN_PROGRESS)
                .withUserId(userId)
                .withAccountId(accountId)
                .withAmount(amount)
                .withPayload(payload == null ? "" : payload);
        return TransactionFactory.createTransaction(type, config);
    }

    private Transaction runStrategy(TransactionType type, UUID userId, UUID accountId, double amount, String payload) throws BankSystemException {
        TransactionStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new BankSystemException();
        }
        return strategy.process(userId, accountId, amount, payload);
    }

    private void notifyObservers(Transaction transaction) {
        for (TransactionObserver observer : observers) {
            observer.onTransaction(transaction);
        }
    }

    private boolean validateUserAccount (UUID userId, UUID accountId) throws BankSystemException {
        User user = getUserByUUID(userId);
        Account account = getAccount(accountId);
        if (account == null || user == null) {
            return false;
        }
        return user.getAccounts().contains(accountId);
    }

    private <T> T withAccountLock(UUID accountId, Callable<T> action) throws BankSystemException {
        Object lock = accountLocks.computeIfAbsent(accountId, k -> new Object());
        synchronized (lock) {
            try {
                return action.call();
            } catch (BankSystemException e) {
                throw e;
            } catch (Exception e) {
                throw new BankSystemException();
            }
        }
    }

    private <T> T withTwoAccountLocks(UUID first, UUID second, Callable<T> action) throws BankSystemException {
        List<UUID> ordered = new ArrayList<>();
        ordered.add(first);
        ordered.add(second);
        ordered.sort(Comparator.comparing(UUID::toString));
        Object lockA = accountLocks.computeIfAbsent(ordered.get(0), k -> new Object());
        Object lockB = accountLocks.computeIfAbsent(ordered.get(1), k -> new Object());
        synchronized (lockA) {
            synchronized (lockB) {
                try {
                    return action.call();
                } catch (BankSystemException e) {
                    throw e;
                } catch (Exception e) {
                    throw new BankSystemException();
                }
            }
        }
    }

    private Server() throws BankSystemException {
        userRepository = new UserRepository("jdbc:sqlite:data/app.db");
        try {
            userRepository.ensureSchema();
        } catch (SQLException e) {
            throw new BankSystemException();
        }
        addObserver(consoleLogger);
        strategies.put(TransactionType.DEPOSIT, (u, a, amt, p) -> processDeposit(u, a, amt));
        strategies.put(TransactionType.WITHDRAW, (u, a, amt, p) -> processWithdraw(u, a, amt));
        strategies.put(TransactionType.FREEZE, (u, a, amt, p) -> processFreeze(u, a));
        strategies.put(TransactionType.TRANSFER, (u, a, amt, p) -> processTransfer(u, a, UUID.fromString(p), amt));
    }
}
