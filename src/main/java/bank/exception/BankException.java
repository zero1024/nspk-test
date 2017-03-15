package bank.exception;

/**
 * @author Perekhod Oleg
 */
public abstract class BankException extends RuntimeException {
    public BankException(String msg) {
        super(msg);
    }
}
