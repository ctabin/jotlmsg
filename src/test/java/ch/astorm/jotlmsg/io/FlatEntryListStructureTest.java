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

public class FlatEntryListStructureTest {

    @Test
    public void testFlatEntryListStructure1() throws IOException {
        try(InputStream msg = OneOffEntryIDStructureTest.class.getResourceAsStream("../msoutlook/replyto.msg");
            MAPIMessage mapiMessage = new MAPIMessage(msg)) {
            
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
            FlatEntryListStructure<FlatEntryStructure> fels1 = new FlatEntryListStructure<>(FlatEntryStructure.class, msgBytes);
            byte[] newBytes = fels1.toBytes();
            FlatEntryListStructure<FlatEntryStructure> fels2 = new FlatEntryListStructure<>(FlatEntryStructure.class, newBytes);
            
            assertNotNull(fels1);
            assertNotNull(fels2);
            assertEquals(fels1.getCount(), fels2.getCount());
            assertEquals(fels1.getSize(), fels2.getSize());
            
            Iterator<FlatEntryStructure> ooels1Iterator = fels1.iterator();
            Iterator<FlatEntryStructure> ooels2Iterator = fels2.iterator();
            while(ooels1Iterator.hasNext() && ooels2Iterator.hasNext()) {
            	FlatEntryStructure ooe1 = ooels1Iterator.next();
            	FlatEntryStructure ooe2 = ooels2Iterator.next();
                assertEquals(ooe1.getSize(), ooe2.getSize());
                assertArrayEquals(ooe1.getEntryID(), ooe2.getEntryID());
            }
        }
    }
	
    @Test
    public void testFlatEntryListStructure2() throws IOException {
        try(InputStream msg = FlatEntryListStructureTest.class.getResourceAsStream("../msoutlook/replyto.msg");
            MAPIMessage mapiMessage = new MAPIMessage(msg)) {
            
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
            
            FlatEntryListStructure<FlatEntryStructure> fels = new FlatEntryListStructure<>(FlatEntryStructure.class, msgBytes);
            assertEquals(2, fels.getCount());
            assertEquals(208, fels.getSize());
            
            for (FlatEntryStructure fes : fels) {
                assertEquals(98, fes.getSize());
            }
            
            FlatEntryStructure fes = new FlatEntryStructure();
            byte[] entryID = {1, 2, 3, 4};
            fes.setEntryID(entryID);
            fels.addFlatEntryStructure(fes);
            assertEquals(entryID.length, fes.getSize());
            
            assertEquals(3, fels.getCount());
        }
    }
    
    @Test
    public void testFlatEntryListStructure3() throws IOException {
        FlatEntryListStructure<FlatEntryStructure> fels = new FlatEntryListStructure<>();
        fels.addFlatEntryStructure(new FlatEntryStructure());
        fels.addFlatEntryStructure(new OneOffEntryIDStructure("test@test.com"));
        assertEquals(2, fels.getCount());
    }
    
    @Test
    public void testFlatEntryListStructure4() throws IOException {
        FlatEntryListStructure<OneOffEntryIDStructure> fels = new FlatEntryListStructure<>();
        fels.addFlatEntryStructure(new OneOffEntryIDStructure("test@test.com"));
        assertEquals(1, fels.getCount());
    }
}
