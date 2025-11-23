package bank.database;

import bank.transaction.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionReader {
    public static Transaction getTransaction(Connection conn, UUID transactionId) throws SQLException {
        String sql = "SELECT t.id, t.timestamp, t.user_id, t.account_id, t.type, t.state, t.amount, t.payload, td.destination_account_id " +
                "FROM transactions t " +
                "LEFT JOIN transfer_details td ON t.id = td.transaction_id " +
                "WHERE t.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transactionId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                TransactionConfig config = readConfig(rs);
                return TransactionFactory.createTransaction(rs.getString("type"), config);
            }
        }
    }

    public static List<Transaction> findTransactions(Connection conn, int limit, int offset) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT t.id, t.timestamp, t.user_id, t.account_id, t.type, t.state, t.amount, t.payload, td.destination_account_id " +
                "FROM transactions t " +
                "LEFT JOIN transfer_details td ON t.id = td.transaction_id " +
                "ORDER BY t.timestamp DESC LIMIT ? OFFSET ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TransactionConfig config = readConfig(rs);
                    transactions.add(TransactionFactory.createTransaction(rs.getString("type"), config));
                }
            }
        }
        return transactions;
    }

    private static TransactionConfig readConfig(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        long tsMillis = rs.getLong("timestamp");
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        State state = State.valueOf(rs.getString("state"));
        UUID userId = UUID.fromString(rs.getString("user_id"));
        UUID accountId = UUID.fromString(rs.getString("account_id"));
        double amount = rs.getDouble("amount");
        boolean amountWasNull = rs.wasNull();
        String payload = rs.getString("payload");

        TransactionConfig config = new TransactionConfig()
                .withUuid(id)
                .withTimestamp(new java.util.Date(tsMillis))
                .withTransactionType(type)
                .withState(state)
                .withUserId(userId)
                .withAccountId(accountId)
                .withPayload(payload == null ? "" : payload);

        if (!amountWasNull) {
            config.withAmount(amount);
        }

        if (type == TransactionType.TRANSFER) {
            String dest = rs.getString("destination_account_id");
            if (dest != null) {
                String newPayload = (payload == null ? "" : payload + ";") + "destinationAccount=" + dest;
                config.withPayload(newPayload);
            }
        }
        return config;
    }
}
