
package ch.astorm.jotlmsg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.util.IOUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OutlookMessageMSGTest {
    
    @Test
    public void testSimpleMessage() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.\nFind some accents: àïâç&@+\"{}$");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        testMessage(message);
    }
    
    @Test
    public void testBaseMessage() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is the subject");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");

        testMessage(message);
    }
    
    @Test
    public void testMessageWithAttachment() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "ctabin@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.CC, "cc@jotlmsg.com", "Copy");
        message.addAttachment("message.txt", "text/plain", new ByteArrayInputStream("Hello, World!".getBytes("UTF-8")));
        
        testMessage(message);
    }
    
    @Test
    public void testMessageWithAttachment2() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "ctabin@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.CC, "cc@jotlmsg.com", "Copy");
        message.addAttachment("message.txt", "text/plain", new ByteArrayInputStream("Hello, World!".getBytes("UTF-8")));
        message.addAttachment("message2.txt", "text/plain", new ByteArrayInputStream("Another attachment with content".getBytes("UTF-8")));
        message.addAttachment("message3.txt", "text/html", new ByteArrayInputStream("<html><body>Some html page</body></html>".getBytes("UTF-8")));
        
        testMessage(message);
    }
    
    @Test
    public void testMessageWithoutAttachment() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "ctabin@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.CC, "cc@jotlmsg.com", "Copy");

        testMessage(message);
    }
    
    private void testMessage(OutlookMessage source) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        source.writeTo(baos);
        baos.close();
        
        OutlookMessage parsed_tmp = new OutlookMessage(new ByteArrayInputStream(baos.toByteArray()));
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        parsed_tmp.writeTo(baos2);
        baos2.close();
        
        OutlookMessage parsed = new OutlookMessage(new ByteArrayInputStream(baos2.toByteArray()));
        
        assertEquals(source.getSubject(), parsed.getSubject());
        assertEquals(source.getFrom(), parsed.getFrom());
        assertEquals(source.getPlainTextBody(), parsed.getPlainTextBody());
        assertEquals(source.getAllRecipients().size(), parsed.getAllRecipients().size());
        assertEquals(source.getAttachments().size(), parsed.getAttachments().size());
        
        List<OutlookMessageRecipient> srcRecipients = source.getAllRecipients();
        List<OutlookMessageRecipient> parsedRecipients = source.getAllRecipients();
        for(int i=0 ; i<srcRecipients.size() ; ++i) {
            OutlookMessageRecipient srcRecipient = srcRecipients.get(i);
            OutlookMessageRecipient parsedRecipient = parsedRecipients.get(i);
            assertEquals(srcRecipient.getType(), parsedRecipient.getType());
            assertEquals(srcRecipient.getName(), parsedRecipient.getName());
            assertEquals(srcRecipient.getEmail(), parsedRecipient.getEmail());
        }
        
        List<OutlookMessageAttachment> srcAttachments = source.getAttachments();
        List<OutlookMessageAttachment> parsedAttachments = source.getAttachments();
        for(int i=0 ; i<srcAttachments.size() ; ++i) {
            OutlookMessageAttachment srcAttachment = srcAttachments.get(i);
            OutlookMessageAttachment parsedAttachment = parsedAttachments.get(i);
            assertEquals(srcAttachment.getName(), parsedAttachment.getName());
            assertEquals(srcAttachment.getMimeType(), parsedAttachment.getMimeType());
            
            byte[] srcData = IOUtils.toByteArray(srcAttachment.getNewInputStream());
            byte[] parData = IOUtils.toByteArray(srcAttachment.getNewInputStream());
            assertEquals(srcData.length, parData.length);
            assertEquals(new String(srcData, "UTF-8"), new String(parData, "UTF-8"));
        }
    }
}
