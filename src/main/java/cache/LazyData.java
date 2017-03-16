package cache;

import java.util.function.Supplier;

/**
 * Контейнер кэшируемых данных, использующий lazy загрузку данных (например из файловой системы)
 *
 * @author Perekhod Oleg
 */
public class LazyData implements Data {

    private final Supplier<byte[]> supplier;
    private volatile boolean isActive = false;

    public LazyData(Supplier<byte[]> supplier) {
        this.supplier = supplier;
    }

    @Override
    public byte[] getData() {
        int tries = 500;
        while (!isActive) {
            sleep10ms();
            //если через 5 секунд ожидания так и не удалось получить доступ к файлу, то возращаем null
            if (tries-- == 0) {
                return null;
            }
        }
        return supplier.get();
    }

    /**
     * По умолчанию данные не доступны. Нужна ручная активация
     */
    public void activate() {
        this.isActive = true;
    }

    private static void sleep10ms() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
