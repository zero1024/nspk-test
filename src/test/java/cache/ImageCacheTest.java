package cache;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Perekhod Oleg
 */
public class ImageCacheTest {

    @Test
    //тестируем простые put и get
    public void testPutGet() throws Exception {
        ImageCache cache = new ImageCacheWithoutFileSystem();
        assert cache.getFromCache(1) == null;
        int id = cache.putToCache(new byte[]{0, 2, 3, 7, 19});
        assert Arrays.equals(cache.getFromCache(id), new byte[]{0, 2, 3, 7, 19});
    }

    @Test
    //тестируем превышение лимита
    public void testFileSystem() throws Exception {
        //ставим лимит 10 байт
        ImageCacheWithFakeFileSystem cache = new ImageCacheWithFakeFileSystem(10);
        //кладем 5 байт
        int i1 = cache.putToCache(new byte[]{0, 2, 3, 7, 19});
        //кладем 4 байта
        int i2 = cache.putToCache(new byte[]{2, 3, 4, 1});
        //кладем 2 байта
        int i3 = cache.putToCache(new byte[]{1, 3});

        assert Arrays.equals(cache.getFromCache(i1), new byte[]{0, 2, 3, 7, 19});
        assert Arrays.equals(cache.getFromCache(i2), new byte[]{2, 3, 4, 1});
        assert Arrays.equals(cache.getFromCache(i3), new byte[]{1, 3});
        assert cache.fileSystem.size() == 1;
        assert Arrays.equals(cache.fileSystem.get(i3 + ""), new byte[]{1, 3});
    }

    @Test
    //тестируем ситуацию когда сохранение в файловую систему происходит медленно
    public void testWithSlowFileSystem() throws Exception {
        ImageCacheWithFakeFileSystem cache = new ImageCacheWithFakeFileSystem(3);
        //ломаем файловую систему
        cache.fileSystemIsWorking.set(false);
        int i1 = cache.putToCache(new byte[]{2, 3, 4, 1});
        Future<byte[]> future = Executors.newSingleThreadExecutor().submit(() -> cache.getFromCache(i1));
        assert !future.isDone();
        sleep(100);
        assert !future.isDone();
        //чиним файловую систему
        cache.fileSystemIsWorking.set(true);
        sleep(100);
        assert future.isDone();
        assert Arrays.equals(future.get(), new byte[]{2, 3, 4, 1});

        //ситуация когда сохранение в файловую систему так и не происходит
        cache.fileSystemIsWorking.set(false);
        int i2 = cache.putToCache(new byte[]{2, 3, 4, 1});
        assert cache.getFromCache(i2) == null;

    }

    @Test
    //В конкурентном режиме грузим кэш. В случае если not-thread-safe методы "придет" больше одного потока, то получим ошибку.
    public void testConcurrent() throws Exception {
        ImageCache cache = new ImageCacheWithNotThreadSafeFileSystem(20);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    int id = cache.putToCache(new byte[]{2, 3});
                    assert Arrays.equals(cache.getFromCache(id), new byte[]{2, 3});
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }


    //--------------------------UTIL------------------------------//


    private static class ImageCacheWithNotThreadSafeFileSystem extends ImageCache {

        private final Map<String, byte[]> fileSystem = new HashMap<>();
        private boolean inWork = false;

        public ImageCacheWithNotThreadSafeFileSystem(int memoryLimit) {
            super(memoryLimit);
        }

        @Override
        protected byte[] loadFromFile(String filename) {
            try {
                if (inWork) {
                    throw new AssertionError();
                }
                inWork = true;
                return fileSystem.get(filename);
            } finally {
                inWork = false;
            }
        }

        @Override
        protected void saveToFile(String filename, byte[] data) {
            try {
                if (inWork) {
                    throw new AssertionError();
                }
                inWork = true;
                fileSystem.put(filename, data);
            } finally {
                inWork = false;
            }
        }
    }

    private static class ImageCacheWithFakeFileSystem extends ImageCache {

        private final Map<String, byte[]> fileSystem = new HashMap<>();
        private final AtomicBoolean fileSystemIsWorking = new AtomicBoolean(true);


        public ImageCacheWithFakeFileSystem(int memoryLimit) {
            super(memoryLimit);
        }

        @Override
        protected byte[] loadFromFile(String filename) {
            return fileSystem.get(filename);
        }

        @Override
        protected void saveToFile(String filename, byte[] data) {
            while (!fileSystemIsWorking.get()) {
                sleep(10);
            }
            fileSystem.put(filename, data);
        }
    }

    private static class ImageCacheWithoutFileSystem extends ImageCache {
        @Override
        protected byte[] loadFromFile(String filename) {
            return null;
        }

        @Override
        protected void saveToFile(String filename, byte[] data) {
        }
    }


    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
