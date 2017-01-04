
package ch.astorm.jotlmsg.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.util.LittleEndian;

/**
 * Handles the top level layout in a {@code .msg} file.
 * This class has been mostly copied from {@link org.apache.poi.hsmf.datatypes.MessagePropertiesChunk}.
 * 
 * @author Cedric Tabin
 */
public class MessagePropertiesChunk extends PropertiesChunk {
    private long nextRecipientId;
    private long nextAttachmentId;
    private long recipientCount;
    private long attachmentCount;

    public long getNextRecipientId() { return nextRecipientId; }
    public void setNextRecipientId(long nextRecipientId) { this.nextRecipientId = nextRecipientId; }

    public long getNextAttachmentId() { return nextAttachmentId; }
    public void setNextAttachmentId(long nextAttachmentId) { this.nextAttachmentId = nextAttachmentId; }

    public long getRecipientCount() { return recipientCount; }
    public void setRecipientCount(long recipientCount) { this.recipientCount = recipientCount; }

    public long getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(long attachmentCount) { this.attachmentCount = attachmentCount; }
    
    @Override
    protected List<PropertyValue> writeHeaderData(OutputStream out) throws IOException {
        //header of the top-level
        //page 21, point 2.4.1.1
        
        // 8 bytes of reserved zeros
        out.write(new byte[8]);

        // Nexts and counts
        LittleEndian.putUInt(nextRecipientId, out);
        LittleEndian.putUInt(nextAttachmentId, out);
        LittleEndian.putUInt(recipientCount, out);
        LittleEndian.putUInt(attachmentCount, out);

        // 8 bytes of reserved zeros
        out.write(new byte[8]);
        
        return super.writeHeaderData(out);
   }
}
