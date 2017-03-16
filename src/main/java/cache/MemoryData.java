package cache;

/**
 * Контейнер кэшируемых данных, хранящий данные в памяти
 *
 * @author Perekhod Oleg
 */
public class MemoryData implements Data {

    private final byte[] data;

    public MemoryData(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] getData() {
        return data;
    }

}
