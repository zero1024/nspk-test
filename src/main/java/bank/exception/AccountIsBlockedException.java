package bank.exception;

/**
 * @author Perekhod Oleg
 */
public class AccountIsBlockedException extends BankException {

    public AccountIsBlockedException(String accNumber) {
        super("Account [" + accNumber + "] is blocked!");
    }
}
