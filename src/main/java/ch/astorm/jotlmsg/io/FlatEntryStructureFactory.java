package ch.astorm.jotlmsg.io;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

/**
 * Creates FlatEntryStructures.
 * 
 * @author Guido Stein
 */
public class FlatEntryStructureFactory<T extends FlatEntryStructure> {
    public T createFlatEntryStructure(Class<T> type, ByteBuffer bf) {
        try {
            return type.getDeclaredConstructor(new Class[] { ByteBuffer.class }).newInstance(bf);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
