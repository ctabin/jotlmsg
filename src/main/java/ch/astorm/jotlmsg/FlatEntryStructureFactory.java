package ch.astorm.jotlmsg;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

/**
 * Creates FlatEntryStructures.
 * 
 * @author Guido Stein
 *
 */
public class FlatEntryStructureFactory<T extends FlatEntryStructure> {
	public T createFlatEntryStructure(Class<T> type, ByteBuffer bf) {
		T fes = null;
		try {
			fes = type.getDeclaredConstructor(new Class[] {ByteBuffer.class}).newInstance(bf);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return fes;
	}
}
