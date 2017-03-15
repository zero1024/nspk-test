package bank.exception;

/**
 * @author Perekhod Oleg
 */
public class NotEnoughMoneyException extends BankException {

    public NotEnoughMoneyException(String accNumber, long money) {
        super("Account [" + accNumber + "] doesn't have enough  money - [" + money + "]");
    }
}
