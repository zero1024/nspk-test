package bank.exception;

/**
 * @author Perekhod Oleg
 */
public class AccountNotFoundException extends BankException {

    public AccountNotFoundException(String accNumber) {
        super("Account [" + accNumber + "] not found!");
    }
}
