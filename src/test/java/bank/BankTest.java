package bank;

import bank.exception.NotEnoughMoneyException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Простые тесты для проверки базовой функциональности
 *
 * @author Perekhod Oleg
 */
public class BankTest {


    @Test(expected = NotEnoughMoneyException.class)
    public void testTransfer() throws Exception {
        Bank bank = new Bank();
        String account1 = bank.createAccount(10);
        String account2 = bank.createAccount(10);

        //0. начальное состояние
        assert bank.getBalance(account1) == 10;
        assert bank.getBalance(account2) == 10;

        //1. успешный перевод
        bank.transfer(account1, account2, 3);
        assert bank.getBalance(account1) == 7;
        assert bank.getBalance(account2) == 13;

        //2. еще один успешный перевод
        bank.transfer(account2, account1, 10);
        assert bank.getBalance(account1) == 17;
        assert bank.getBalance(account2) == 3;

        //3. ошибка!
        bank.transfer(account2, account1, 10);
        assert bank.getBalance(account1) == 17;
        assert bank.getBalance(account2) == 3;
    }

    @Test
    public void testConcurrent() throws Exception {
        Random random = new Random();
        Bank bank = new Bank();
        List<String> accounts = new ArrayList<>();
        int allMoney = 0;

        //1. создает 100 аккаунтов
        for (int i = 0; i < 1000; i++) {
            int money = random.nextInt(100000);
            allMoney += money;
            String accNum = bank.createAccount(money);
            accounts.add(accNum);
        }

        //2. запускаем 1000 потоков которые делают случайные переводы
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Thread thread = new Thread(() -> {
                for (int i1 = 0; i1 < 10; i1++) {
                    String from = accounts.get(random.nextInt(1000));
                    String to = accounts.get(random.nextInt(1000));
                    bank.transfer(from, to, random.nextInt(100000));
                    sleep(random.nextInt(2000));
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        //3. проверяем что суммарное число денег не изменилось
        int allMoneyAfterTest = 0;
        int blockedCount = 0;
        for (String account : accounts) {
            allMoneyAfterTest += bank.getBalance(account);
            if (bank.isBlocked(account)) {
                blockedCount++;
            }
        }

        assert allMoney == allMoneyAfterTest;

        System.out.println(String.format("Test result:\r\n BlockedCount - [%s] \r\n AllMoney - [%s]", blockedCount, allMoney));

    }


    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
