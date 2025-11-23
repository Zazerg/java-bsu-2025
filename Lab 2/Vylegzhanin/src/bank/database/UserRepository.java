package bank.database;

import bank.Account;
import bank.ActionResult;
import bank.User;
import bank.transaction.Transaction;
import bank.transaction.TransactionConfig;
import bank.transaction.TransactionFactory;
import bank.transaction.TransactionType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepository {
    private final String jdbcUrl;

    public UserRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void ensureSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL" +
                    ")");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (" +
                    "id TEXT PRIMARY KEY," +
                    "user_id TEXT NOT NULL REFERENCES users(id)," +
                    "balance REAL NOT NULL DEFAULT 0," +
                    "frozen INTEGER NOT NULL DEFAULT 0" +
                    ")");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id TEXT PRIMARY KEY," +
                    "timestamp INTEGER NOT NULL," +
                    "user_id TEXT NOT NULL REFERENCES users(id)," +
                    "account_id TEXT NOT NULL REFERENCES accounts(id)," +
                    "type TEXT NOT NULL," +
                    "state TEXT NOT NULL," +
                    "amount REAL," +
                    "payload TEXT," +
                    "CHECK ((type NOT IN ('FREEZE') AND amount IS NOT NULL) " +
                    "OR (type IN ('FREEZE') AND amount IS NULL))" +
                    ")");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transfer_details (" +
                    "transaction_id TEXT PRIMARY KEY REFERENCES transactions(id) ON DELETE CASCADE," +
                    "destination_account_id TEXT NOT NULL REFERENCES accounts(id)" +
                    ")");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tx_user ON transactions(user_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tx_account ON transactions(account_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tx_amount ON transactions(amount)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transfer_destination ON transfer_details(destination_account_id)");

            try { stmt.executeUpdate("ALTER TABLE accounts ADD COLUMN frozen INTEGER NOT NULL DEFAULT 0"); } catch (SQLException ignore) {}
            try { stmt.executeUpdate("ALTER TABLE transactions ADD COLUMN payload TEXT"); } catch (SQLException ignore) {}
            try { stmt.executeUpdate("ALTER TABLE transactions ADD COLUMN amount REAL"); } catch (SQLException ignore) {}
            try { stmt.executeUpdate("UPDATE accounts SET frozen = COALESCE(frozen, 0)"); } catch (SQLException ignore) {}
        }
    }

    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users(id, name) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId().toString());
            stmt.setString(2, user.getName());
            stmt.executeUpdate();
        }
        user.makeValid();
    }

    public void addAccount(UUID accountId, UUID userId, double balance) throws SQLException {
        String sql = "INSERT INTO accounts(id, user_id, balance, frozen) VALUES (?, ?, ?, 0)";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId.toString());
            stmt.setString(2, userId.toString());
            stmt.setDouble(3, balance);
            stmt.executeUpdate();
        }
    }

    public Account getAccount(UUID accountId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
        PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(accountId, rs.getDouble("balance"), rs.getInt("frozen") != 0);
                }
            }
        }
        return null;
    }

    public void updateAccountBalance(UUID accountId, double balance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, balance);
            stmt.setString(2, accountId.toString());
            stmt.executeUpdate();
        }
    }

    public void updateAccountFrozen(UUID accountId, boolean frozen) throws SQLException {
        String sql = "UPDATE accounts SET frozen = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, frozen ? 1 : 0);
            stmt.setString(2, accountId.toString());
            stmt.executeUpdate();
        }
    }

    public List<UUID> findAccountIdsByUser(UUID userId) throws SQLException {
        List<UUID> accountIds = new ArrayList<>();
        String sql = "SELECT id FROM accounts WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accountIds.add(UUID.fromString(rs.getString("id")));
                }
            }
        }
        return accountIds;
    }

    public List<UUID> findAccountIdsByPrefix(String prefix, int limit) throws SQLException {
        List<UUID> accountIds = new ArrayList<>();
        String sql = "SELECT id FROM accounts WHERE id LIKE ? ORDER BY id LIMIT ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prefix + "%");
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accountIds.add(UUID.fromString(rs.getString("id")));
                }
            }
        }
        return accountIds;
    }

    public List<UUID> findAllAccountIds() throws SQLException {
        List<UUID> accountIds = new ArrayList<>();
        String sql = "SELECT id FROM accounts";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                accountIds.add(UUID.fromString(rs.getString("id")));
            }
        }
        return accountIds;
    }

    public String findUserNameById(UUID userId) throws SQLException {
        String sql = "SELECT name FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        return null;
    }

    public List<User> findUsers(String nameLike, int limit, int offset) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name FROM users WHERE name LIKE ? ORDER BY name LIMIT ? OFFSET ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nameLike == null ? "%" : nameLike);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String name = rs.getString("name");
                    User user = new User(id, name);
                    users.add(user);
                }
            }
        }
        return users;
    }

    public void addTransaction(Transaction transaction) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                TransactionWriter writer = new TransactionWriter(conn);
                transaction.visit(writer);
                if (writer.getResult() == ActionResult.SUCCESS) {
                    conn.commit();
                } else {
                    conn.rollback();
                    throw new SQLException();
                }
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public Transaction findTransactionById(UUID transactionId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            return TransactionReader.getTransaction(conn, transactionId);
        }
    }

    public List<Transaction> findTransactions (int limit, int offset) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            return TransactionReader.findTransactions(conn, limit, offset);
        }
    }

    public void updateTransaction(Transaction transaction) throws SQLException {
        String sql = "UPDATE transactions SET state = ?, timestamp = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transaction.getState().name());
            stmt.setLong(2, transaction.getTimestamp().getTime());
            stmt.setString(3, transaction.getUuid().toString());
            stmt.executeUpdate();
        }
    }
}
