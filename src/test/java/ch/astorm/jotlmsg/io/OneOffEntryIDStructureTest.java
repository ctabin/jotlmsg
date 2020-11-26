package ch.astorm.jotlmsg.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.ByteChunk;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.junit.jupiter.api.Test;

public class OneOffEntryIDStructureTest {

    @Test
    public void testOneOffEntryIDStructure1() throws IOException {
        InputStream msg = OneOffEntryIDStructureTest.class.getResourceAsStream("../msoutlook/replyto.msg");
        MAPIMessage mapiMessage = new MAPIMessage(msg);

        byte[] msgBytes = null;
        
        List<Chunk> replyEntriesChunks = mapiMessage.getMainChunks().getAll().get(MAPIProperty.REPLY_RECIPIENT_ENTRIES);
        if(replyEntriesChunks!=null && !replyEntriesChunks.isEmpty()) {
            for(Chunk chunk : replyEntriesChunks) {
                if(chunk instanceof ByteChunk) {
                    ByteChunk bc = (ByteChunk)chunk;
                    msgBytes = bc.getValue();
                }
            }
        }
        assertNotNull(msgBytes);
        FlatEntryListStructure<OneOffEntryIDStructure> fels = new FlatEntryListStructure<>(OneOffEntryIDStructure.class, msgBytes);
        
        assertNotNull(fels);
        assertEquals(2, fels.getCount());
        assertEquals(208, fels.getSize());
        
        List<OneOffEntryIDStructure> ooels = fels.getFlatEntryStructures();
        assertNotNull(ooels);
        OneOffEntryIDStructure ooe1 = ooels.get(0);
        assertEquals(98, ooe1.getSize());
        assertEquals("reply1@test.com", ooe1.getDisplayName());
        assertEquals("reply1@test.com", ooe1.getEmailAddress());
        OneOffEntryIDStructure ooe2 = ooels.get(1);
        assertEquals(98, ooe2.getSize());
        assertEquals("reply2@test.com", ooe2.getDisplayName());
        assertEquals("reply2@test.com", ooe2.getEmailAddress());
        
        mapiMessage.close();
        msg.close();
    }

    @Test
    public void testOneOffEntryIDStructure2() throws IOException {
        InputStream msg = OneOffEntryIDStructureTest.class.getResourceAsStream("../msoutlook/replyto.msg");
        MAPIMessage mapiMessage = new MAPIMessage(msg);
        
        byte[] msgBytes = null;
        
        List<Chunk> replyEntriesChunks = mapiMessage.getMainChunks().getAll().get(MAPIProperty.REPLY_RECIPIENT_ENTRIES);
        if(replyEntriesChunks!=null && !replyEntriesChunks.isEmpty()) {
            for(Chunk chunk : replyEntriesChunks) {
                if(chunk instanceof ByteChunk) {
                    ByteChunk bc = (ByteChunk)chunk;
                    msgBytes = bc.getValue();
                }
            }
        }
        assertNotNull(msgBytes);
        FlatEntryListStructure<OneOffEntryIDStructure> fels1 = new FlatEntryListStructure<>(OneOffEntryIDStructure.class, msgBytes);
        
        FlatEntryListStructure<OneOffEntryIDStructure> fels2 = new FlatEntryListStructure<>();
        fels2.addFlatEntryStructure(new OneOffEntryIDStructure("reply1@test.com", "reply1@test.com"));
        fels2.addFlatEntryStructure(new OneOffEntryIDStructure("reply2@test.com", "reply2@test.com"));
        byte[] newBytes = fels2.toBytes();
        // assertArrayEquals(msgBytes, newBytes); doesn't work: alignment bytes with random values in msg file!  
        fels2 = new FlatEntryListStructure<OneOffEntryIDStructure>(OneOffEntryIDStructure.class, newBytes);
        
        assertNotNull(fels1);
        assertNotNull(fels2);
        assertEquals(fels1.getCount(), fels2.getCount());
        assertEquals(fels1.getSize(), fels2.getSize());
        
        Iterator<OneOffEntryIDStructure> ooels1Iterator = fels1.iterator();
        Iterator<OneOffEntryIDStructure> ooels2Iterator = fels2.iterator();
        while(ooels1Iterator.hasNext() && ooels2Iterator.hasNext()) {
             OneOffEntryIDStructure ooe1 = ooels1Iterator.next();
             OneOffEntryIDStructure ooe2 = ooels2Iterator.next();
             assertEquals(ooe1.getSize(), ooe2.getSize());
             assertArrayEquals(ooe1.getEntryID(), ooe2.getEntryID());
             assertEquals(ooe1.getDisplayName(), ooe2.getDisplayName());
             assertEquals(ooe1.getEmailAddress(), ooe2.getEmailAddress());
        }
        
        mapiMessage.close();
        msg.close();
    }
    
    @Test
    public void testOneOffEntryIDStructure3() throws IOException {
        FlatEntryListStructure<OneOffEntryIDStructure> fels1 = new FlatEntryListStructure<>();
        fels1.addFlatEntryStructure(new OneOffEntryIDStructure("Reply Address", "reply@test.com"));
        fels1.addFlatEntryStructure(new OneOffEntryIDStructure("reply2@test.com"));
        fels1.addFlatEntryStructure(new OneOffEntryIDStructure("Sales Department", "sales@test.com"));
        fels1.addFlatEntryStructure(new OneOffEntryIDStructure("Production", "production@test.com"));
        fels1.addFlatEntryStructure(new OneOffEntryIDStructure("Joe Grinner", "joe.grinner@test.com"));
        
        byte[] newBytes = fels1.toBytes();
        FlatEntryListStructure<OneOffEntryIDStructure> fels2 = new FlatEntryListStructure<>(OneOffEntryIDStructure.class, newBytes);
        
        assertNotNull(fels1);
        assertNotNull(fels2);
        assertEquals(fels1.getCount(), fels2.getCount());
        assertEquals(fels1.getSize(), fels2.getSize());
        
        Iterator<OneOffEntryIDStructure> ooels1Iterator = fels1.iterator();
        Iterator<OneOffEntryIDStructure> ooels2Iterator = fels1.iterator();
        while(ooels1Iterator.hasNext() && ooels2Iterator.hasNext()) {
             OneOffEntryIDStructure ooe1 = ooels1Iterator.next();
             OneOffEntryIDStructure ooe2 = ooels2Iterator.next();
             assertEquals(ooe1.getSize(), ooe2.getSize());
             assertArrayEquals(ooe1.getEntryID(), ooe2.getEntryID());
             assertEquals(ooe1.getDisplayName(), ooe2.getDisplayName());
             assertEquals(ooe1.getEmailAddress(), ooe2.getEmailAddress());
        }
    }
}
