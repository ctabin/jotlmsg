
package ch.astorm.jotlmsg;

import ch.astorm.jotlmsg.OutlookMessageRecipient.Type;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;


public class OutlookMessageTest {
    
    @Test
    public void testFromScratch() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.addRecipient(Type.TO, "ctabin@jotlmsg.com");
        message.addRecipient(Type.CC, "cc@jotlmsg.com", "Copy");
        message.addAttachment("Empty file", "text/plain", a -> null);
        
        assertEquals("This is a message", message.getSubject());
        assertEquals("Hello,\n\nThis is a simple message.\n\n.Bye.", message.getPlainTextBody());
        assertEquals(2, message.getRecipients(Type.TO).size());
        assertEquals(1, message.getRecipients(Type.CC).size());
        assertEquals(0, message.getRecipients(Type.BCC).size());
        assertEquals("cedric@jotlmsg.com", message.getRecipients(Type.TO).get(0).getEmail());
        assertEquals("Cédric", message.getRecipients(Type.TO).get(0).getName());
        assertEquals("ctabin@jotlmsg.com", message.getRecipients(Type.TO).get(1).getEmail());
        assertEquals("cc@jotlmsg.com", message.getRecipients(Type.CC).get(0).getEmail());
        assertEquals("Copy", message.getRecipients(Type.CC).get(0).getName());
        assertNull(message.getRecipients(Type.TO).get(1).getName());
        assertEquals(1, message.getAttachments().size());
        assertEquals("Empty file", message.getAttachments().get(0).getName());
        assertNull(message.getAttachments().get(0).getNewInputStream());
        
        message.removeAllRecipients(Type.TO);
        assertEquals(0, message.getRecipients(Type.TO).size());
        
        message.removeAllRecipients();
        assertEquals(0, message.getRecipients(Type.CC).size());
        assertEquals(0, message.getRecipients(Type.BCC).size());
        
        message.removeAllAttachments();
        assertEquals(0, message.getAttachments().size());
    }
    
    @Test
    public void testParsingSimple1() throws IOException {
        InputStream msg = OutlookMessageTest.class.getResourceAsStream("msoutlook/simple.msg");
        OutlookMessage message = new OutlookMessage(msg);
        
        assertEquals("Test subject", message.getSubject());
        assertEquals("Hello,This is a simple test message.See ya,Tester", message.getPlainTextBody().replaceAll("\r?\n\\s*", ""));
        assertEquals(0, message.getAttachments().size());
        assertEquals(1, message.getRecipients(Type.TO).size());
        assertEquals(1, message.getRecipients(Type.CC).size());
        assertEquals(1, message.getRecipients(Type.BCC).size());
        assertEquals("to@test.com", message.getRecipients(Type.TO).get(0).getEmail());
        assertNull(message.getRecipients(Type.TO).get(0).getName());
        assertEquals("cc@test.com", message.getRecipients(Type.CC).get(0).getEmail());
        assertNull(message.getRecipients(Type.CC).get(0).getName());
        assertEquals("bcc@test.com", message.getRecipients(Type.BCC).get(0).getEmail());
        assertNull(message.getRecipients(Type.BCC).get(0).getName());
    }
    
    @Test
    public void testParsingSimple2() throws IOException {
        InputStream msg = OutlookMessageTest.class.getResourceAsStream("msoutlook/simple2.msg");
        OutlookMessage message = new OutlookMessage(msg);
        
        assertEquals("My subject", message.getSubject());
        assertEquals("Hello, world.", message.getPlainTextBody());
        assertEquals(0, message.getAttachments().size());
        assertEquals(1, message.getRecipients(Type.TO).size());
        assertEquals(0, message.getRecipients(Type.CC).size());
        assertEquals(0, message.getRecipients(Type.BCC).size());
        assertEquals("roger@test.com", message.getRecipients(Type.TO).get(0).getEmail());
        assertNull(message.getRecipients(Type.TO).get(0).getName());
    }
    
    @Test
    public void testParsingAttachment() throws IOException {
        InputStream msg = OutlookMessageTest.class.getResourceAsStream("msoutlook/attachment.msg");
        OutlookMessage message = new OutlookMessage(msg);
        
        assertNull(message.getSubject());
        assertEquals("Mail with attachment and no subject.", message.getPlainTextBody());
        assertEquals(1, message.getAttachments().size());
        assertEquals(1, message.getRecipients(Type.TO).size());
        assertEquals(0, message.getRecipients(Type.CC).size());
        assertEquals(0, message.getRecipients(Type.BCC).size());
        assertEquals("to@test.com", message.getRecipients(Type.TO).get(0).getEmail());
        assertNull(message.getRecipients(Type.TO).get(0).getName());
        
        assertEquals("myAttachement.txt", message.getAttachments().get(0).getName());
        
        String data = IOUtils.toString(message.getAttachments().get(0).getNewInputStream(), "UTF-8");
        assertEquals("This is some basic content of attached file.", data);
    }
}
