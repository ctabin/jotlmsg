
package ch.astorm.jotlmsg.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.hsmf.datatypes.PropertyValue;

/**
 * Handles the writing of a simple storage chunk.
 * This class is based on {@link org.apache.poi.hsmf.datatypes.StoragePropertiesChunk}.
 * 
 * @author Cedric Tabin
 */
public class StoragePropertiesChunk extends PropertiesChunk {
    
    @Override
    protected List<PropertyValue> writeHeaderData(OutputStream out) throws IOException {
        //storage header
        //page 23, point 2.4.1.3

        // 8 bytes of reserved zeros
        out.write(new byte[8]);

        return super.writeHeaderData(out);
    }
}
