package bank.transaction;

public enum State {
    INVALID,
    PREPARING,
    PREPARED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}
