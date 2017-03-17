package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Особенности реализации:
 * <p>
 * В случае если данные не влезают в память, то происходит сохранение данных на диск в отдельном потоке, а пользователю кэша сразу
 * отдается id. Если пользователь тут же запросит эти данные, то он получит блокировку на методе get(), до тех пор пока данные не запишутся на диск.
 *
 * @author Perekhod Oleg
 */

public abstract class ImageCache {

    private final int memoryLimit;
    private final Map<Integer, Data> cache;
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
        this.cache = new ConcurrentHashMap<>();
        this.memoryUsage = new AtomicInteger();
        this.ids = new AtomicInteger();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    //------------Публичное API-----------------//

    public int putToCache(byte[] data) {
        int id = ids.incrementAndGet();
        if (isFitToMemory(data.length)) {
            cache.put(id, new MemoryData(data));
        } else {
            String filename = filename(id);
            LazyData lazyData = createLazyData(filename);
            cache.put(id, lazyData);
            asyncSaveToFile(data, filename, lazyData::activate);
        }
        return id;
    }


    public byte[] getFromCache(int id) {
        return cache.containsKey(id) ? cache.get(id).getData() : null;
    }

    //---------------------UTIL---------------------------//


    private LazyData createLazyData(String filename) {
        return new LazyData(() -> {
            try {
                lock.lock();
                return loadFromFile(filename);
            } finally {
                lock.unlock();
            }
        });
    }

    //Асинхронное сохранение данных в файл. После успешного сохранения происходит вызов finalCallback.
    private void asyncSaveToFile(byte[] data, String filename, Runnable finalCallback) {
        executorService.submit(() -> {
            try {
                lock.lock();
                saveToFile(filename, data);
                finalCallback.run();
            } finally {
                lock.unlock();
            }
        });
    }

    private static String filename(Integer id) {
        return id.toString();
    }

    //Проверяем, что данные влезают в память. В случае если влезают, то обновляем memoryUsage в CAS режиме.
    private boolean isFitToMemory(int dataLength) {
        int prev, next;
        do {
            prev = memoryUsage.get();
            next = prev + dataLength;
            if (next > memoryLimit) {
                return false;
            }
        } while (!memoryUsage.compareAndSet(prev, next));
        return true;
    }


    //----------Работа с файлами--------------------//

    protected abstract byte[] loadFromFile(String filename);

    protected abstract void saveToFile(String filename, byte[] data);

}

