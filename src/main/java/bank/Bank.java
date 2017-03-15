package bank;

import bank.exception.AccountIsBlockedException;
import bank.exception.AccountNotFoundException;
import bank.exception.BankException;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Perekhod Oleg
 */
public class Bank {

    private final Map<String, Account> accountMap = new ConcurrentHashMap<>();
    //нам достаточно одного потока, т.к. служба безопасности все равно работает в synchronized режиме
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    //--------------Публичное API----------------------//

    public String createAccount(long money) {
        String num = UUID.randomUUID().toString();
        accountMap.put(num, new Account(num, money));
        return num;
    }

    public void transfer(String fromAccountNum, String toAccountNum, long amount) throws BankException {

        if (fromAccountNum.equals(toAccountNum)) {
            throw new IllegalArgumentException("What's going on?");
        }

        //1. получаем аккаунты если они есть
        Account fromAccount = getAccount(fromAccountNum);
        Account toAccount = getAccount(toAccountNum);

        //2. проверяем заблокированы ли аккаунты (эти же самые проверки будут происходить во время снятие и добавления денег)
        fromAccount.checkIsBlocked();
        toAccount.checkIsBlocked();

        //3. берем деньги с одного аккаунта
        fromAccount.retrieveMoney(amount);

        try {
            //4.1 переводим деньги
            toAccount.addMoney(amount);
        } catch (AccountIsBlockedException e) {
            //4.2 возвращаем деньги если второй аккаунт заблокирован
            fromAccount.rollbackMoney(amount);
            throw e;
        }

        //5. проверка службы безопасности
        securityCheck(fromAccount, toAccount, amount);

    }

    public long getBalance(String accountNum) {
        return accountMap.get(accountNum).getMoney();
    }

    public boolean isBlocked(String accountNum) {
        return accountMap.get(accountNum).isBlocked();
    }

    //----------------------Служба Безопасности------------------//

    private final static Integer UNSAFE_LIMIT = 50000;
    private final Random random = new Random();

    private void securityCheck(Account fromAccount, Account toAccount, long amount) {
        if (amount > UNSAFE_LIMIT) {
            executorService.submit(() -> {
                if (isFraud(fromAccount.getNum(), toAccount.getNum(), amount)) {
                    fromAccount.block();
                    toAccount.block();
                }
            });
        }
    }

    private synchronized boolean isFraud(String fromAccountNum, String toAccountNum, long amount) {
        try {
            Thread.sleep(1000);
            return random.nextBoolean();
        } catch (InterruptedException e) {
            return true;
        }
    }


    //---------------------UTIL--------------------//

    private Account getAccount(String accountNum) {
        Account res = accountMap.get(accountNum);
        if (res == null) {
            throw new AccountNotFoundException(accountNum);
        }
        return res;
    }


}
