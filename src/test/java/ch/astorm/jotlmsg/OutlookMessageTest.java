
package ch.astorm.jotlmsg;

import ch.astorm.jotlmsg.OutlookMessageRecipient.Type;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;


public class OutlookMessageTest {
    
    @Test
    public void testFromScratch() {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.addRecipient(Type.TO, "ctabin@jotlmsg.com");
        message.addRecipient(Type.CC, "cc@jotlmsg.com", "Copy");
        message.addAttachment("Empty file", null);
        
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
        assertNull(message.getAttachments().get(0).getInputStream());
        
        message.removeAllRecipients(Type.TO);
        assertEquals(0, message.getRecipients(Type.TO).size());
        
        message.removeAllAttachments();
        assertEquals(0, message.getAttachments().size());
    }
    
}
