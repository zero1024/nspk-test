package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author Perekhod Oleg
 */

public abstract class ImageCache {

    private final int memoryLimit;
    private final Map<Integer, Data> map;
    private final AtomicInteger memoryUsage;
    private final AtomicInteger ids;
    private final ExecutorService executorService;
    private final Lock lock = new ReentrantLock();

    /**
     * По умолчанию лимит памяти 100 Мб
     */
    public ImageCache() {
        this(100 * 1024 * 1024);
    }

    public ImageCache(int memoryLimit) {
        this.memoryLimit = memoryLimit;
        this.map = new ConcurrentHashMap<>();
        this.memoryUsage = new AtomicInteger();
        this.ids = new AtomicInteger();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    //------------Публичное API-----------------//

    public int putToCache(byte[] data) {
        int id = ids.incrementAndGet();
        if (isFitToMemory(data)) {
            map.put(id, new MemoryData(data));
        } else {
            String filename = id + "";
            LazyData lazyData = new LazyData(() -> safeCall(() -> loadFromFile(filename)));
            map.put(id, lazyData);
            executorService.submit(() -> safeCall(() -> {
                saveToFile(filename, data);
                lazyData.activate();
            }));
        }
        return id;
    }


    public byte[] getFromCache(int id) {
        return map.containsKey(id) ? map.get(id).getData() : null;
    }

    //--------------Вспомогательные классы---------------------//

    private interface Data {
        byte[] getData();
    }

    private static class LazyData implements Data {

        private final Supplier<byte[]> supplier;
        private volatile boolean isActive = false;

        private LazyData(Supplier<byte[]> supplier) {
            this.supplier = supplier;
        }

        @Override
        public byte[] getData() {
            int tries = 1000;
            while (!isActive) {
                sleep10ms();
                //если через 10 секунд ожидания так и не удалось получить доступ к файлу, то возращаем null
                if (tries-- == 0) {
                    return null;
                }
            }
            return supplier.get();
        }

        private void activate() {
            this.isActive = true;
        }

    }

    private static class MemoryData implements Data {

        private final byte[] data;

        private MemoryData(byte[] data) {
            this.data = data;
        }

        @Override
        public byte[] getData() {
            return data;
        }

    }


    //---------------------UTIL---------------------------//

    private void safeCall(Runnable runnable) {
        safeCall(() -> {
            runnable.run();
            return null;
        });
    }

    private <V> V safeCall(Supplier<V> supplier) {
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }


    private boolean isFitToMemory(byte[] data) {
        int prev, next;
        do {
            prev = memoryUsage.get();
            next = prev + data.length;
            if (next > memoryLimit) {
                return false;
            }
        } while (!memoryUsage.compareAndSet(prev, next));
        return true;
    }

    private static void sleep10ms() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    //----------Работа с файлами--------------------//

    protected abstract byte[] loadFromFile(String filename);

    protected abstract void saveToFile(String filename, byte[] data);

}

