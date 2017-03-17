package bank.exception;

/**
 * @author Perekhod Oleg
 */
public class SameAccountException extends BankException {

    public SameAccountException() {
        super("Transfer on the same account is unacceptable!");
    }
}
