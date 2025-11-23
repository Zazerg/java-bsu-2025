package bank.database;

import bank.ActionResult;
import bank.transaction.TransactionVisitor;
import bank.transaction.transactions.TransactionDeposit;
import bank.transaction.transactions.TransactionFreeze;
import bank.transaction.transactions.TransactionTransfer;
import bank.transaction.transactions.TransactionWithdraw;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

public class TransactionWriter implements TransactionVisitor {
    private final Connection connection;
    private ActionResult result = ActionResult.FAILURE;

    public TransactionWriter(Connection connection) {
        this.connection = connection;
    }

    @Override
    public ActionResult visitDeposit(TransactionDeposit t) {
        result = insertTransaction(t, t.getAmount(), "");
        return result;
    }

    @Override
    public ActionResult visitWithdraw(TransactionWithdraw t) {
        result = insertTransaction(t, t.getAmount(), "");
        return result;
    }

    @Override
    public ActionResult visitFreeze(TransactionFreeze t) {
        result = insertTransaction(t, null, "");
        return result;
    }

    @Override
    public ActionResult visitTransfer(TransactionTransfer t) {
        ActionResult txResult = insertTransaction(t, t.getAmount(), "destinationAccount=" + t.getDestinationAccount());
        if (txResult == ActionResult.FAILURE) {
            result = ActionResult.FAILURE;
            return result;
        }

        String transferSql = "INSERT INTO transfer_details (transaction_id, destination_account_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(transferSql)) {
            stmt.setString(1, t.getUuid().toString());
            stmt.setString(2, t.getDestinationAccount().toString());
            stmt.executeUpdate();
            result = ActionResult.SUCCESS;
        } catch (Exception e) {
            result = ActionResult.FAILURE;
        }
        return result;
    }

    public ActionResult getResult() {
        return result;
    }

    private ActionResult insertTransaction(bank.transaction.Transaction t, Double amount, String payload) {
        String sql = "INSERT INTO transactions (id, timestamp, user_id, account_id, type, state, amount, payload) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindCommon(stmt, t);
            if (amount == null) {
                stmt.setNull(7, Types.REAL);
            } else {
                stmt.setDouble(7, amount);
            }
            stmt.setString(8, payload);
            stmt.executeUpdate();
            return ActionResult.SUCCESS;
        } catch (Exception e) {
            return ActionResult.FAILURE;
        }
    }

    private void bindCommon(PreparedStatement stmt, bank.transaction.Transaction t) throws Exception {
        stmt.setString(1, t.getUuid().toString());
        long ts = t.getTimestamp().getTime();
        stmt.setLong(2, ts);
        stmt.setString(3, t.getUserId().toString());
        stmt.setString(4, t.getAccountId().toString());
        stmt.setString(5, t.getTransactionType().name());
        stmt.setString(6, t.getState().name());
    }
}
