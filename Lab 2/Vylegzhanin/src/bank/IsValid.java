package bank;

public abstract class IsValid {
    private boolean valid = false;

    public boolean isValid() {
        return valid;
    }
    public void makeValid() {
        this.valid = true;
    }
}
