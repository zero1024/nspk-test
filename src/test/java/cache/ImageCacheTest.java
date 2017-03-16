package cache;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Perekhod Oleg
 */
public class ImageCacheTest {

    @Test
    public void testPutGet() throws Exception {
        ImageCache cache = new ImageCacheWithoutFileSystem();
        assert cache.getFromCache(1) == null;
        int id = cache.putToCache(new byte[]{0, 2, 3, 7, 19});
        assert Arrays.equals(cache.getFromCache(id), new byte[]{0, 2, 3, 7, 19});
    }

    @Test
    public void testFileSystem() throws Exception {
        //лимит 10 байт
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
        sleep(10);
        assert cache.fileSystem.size() == 1;
        assert Arrays.equals(cache.fileSystem.get(i3 + ""), new byte[]{1, 3});
    }

    @Test
    public void testWithSlowFileSystem() throws Exception {
        ImageCacheWithFakeFileSystem cache = new ImageCacheWithFakeFileSystem(3);
        cache.fileSystemIsWorking.set(false);
        int i1 = cache.putToCache(new byte[]{2, 3, 4, 1});
        Future<byte[]> future = Executors.newSingleThreadExecutor().submit(() -> cache.getFromCache(i1));
        assert !future.isDone();
        sleep(100);
        assert !future.isDone();
        cache.fileSystemIsWorking.set(true);
        sleep(100);
        assert future.isDone();
        assert Arrays.equals(future.get(), new byte[]{2, 3, 4, 1});


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

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
}
