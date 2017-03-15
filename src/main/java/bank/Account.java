package bank;

import bank.exception.AccountIsBlockedException;
import bank.exception.BankException;
import bank.exception.NotEnoughMoneyException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Perekhod Oleg
 */
public class Account {

    private final String num;
    private final AtomicLong money;
    private final AtomicBoolean blocked;

    public Account(String num, long money) {
        this.money = new AtomicLong(money);
        this.blocked = new AtomicBoolean(false);
        this.num = num;
    }

    public void block() {
        this.blocked.set(true);
    }

    public String getNum() {
        return num;
    }

    public long getMoney() {
        return money.get();
    }

    /**
     * Снимаем деньги с аккаунта
     */
    public void retrieveMoney(long amount) throws BankException {
        money.updateAndGet(money -> {
            checkIsBlocked();
            if (money < amount) {
                throw new NotEnoughMoneyException(num, amount);
            }
            return money - amount;
        });
    }

    /**
     * Добавляем деньги на аккаунт
     */
    public void addMoney(long amount) throws AccountIsBlockedException {
        money.updateAndGet(money -> {
            checkIsBlocked();
            return money + amount;
        });
    }

    /**
     * Возвращаем деньги на аккаунт
     */
    public void rollbackMoney(long amount) {
        money.updateAndGet(money -> money + amount);
    }


    public void checkIsBlocked() throws AccountIsBlockedException {
        if (blocked.get()) {
            throw new AccountIsBlockedException(num);
        }
    }

    public boolean isBlocked() {
        return blocked.get();
    }


}
