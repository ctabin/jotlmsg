
package ch.astorm.jotlmsg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class OutlookMessageMIMETest {
    
    @Test
    public void testBasicGeneration() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        
        try { MimeMessage mimeMessage = message.toMimeMessage(); assertFalse(true); }
        catch(MessagingException me) { }
        
        message.setPlainTextBody("Hello, World!");
        
        MimeMessage mimeMessage1 = message.toMimeMessage();
        
        try(ByteArrayOutputStream baos1 = new ByteArrayOutputStream()) {
            mimeMessage1.writeTo(baos1);
        }
        
        assertNull(mimeMessage1.getFrom());
        assertNull(mimeMessage1.getSubject());
        assertNull(mimeMessage1.getReplyTo());
        assertEquals(1, mimeMessage1.getRecipients(Message.RecipientType.TO).length);
        assertNull(mimeMessage1.getRecipients(Message.RecipientType.CC));
        assertNull(mimeMessage1.getRecipients(Message.RecipientType.BCC));
        
        Multipart multipart1 = (Multipart)mimeMessage1.getContent();
        assertEquals(1, multipart1.getCount());
        assertEquals("body", multipart1.getBodyPart(0).getFileName());
        
        String body1 = IOUtils.toString(multipart1.getBodyPart(0).getInputStream(), StandardCharsets.UTF_8);
        assertEquals(message.getPlainTextBody(), body1);
    }
    
    @Test
    public void testSimpleDoubleGeneration() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setReplyTo(Arrays.asList("reply1@jotlmsg.com", "reply2@jotlmsg.com"));
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "ctabin@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.CC, "cc@jotlmsg.com", "Copy");
        message.addAttachment("message.txt", "text/plain", new ByteArrayInputStream("Hello, World!".getBytes(StandardCharsets.UTF_8)));
        
        // -----------------------------------------------------
        
        MimeMessage mimeMessage1 = message.toMimeMessage();
        
        try(ByteArrayOutputStream baos1 = new ByteArrayOutputStream()) {
            mimeMessage1.writeTo(baos1);
        }
        
        assertEquals(1, mimeMessage1.getFrom().length);
        assertEquals("sender@jotlmsg.com", mimeMessage1.getFrom()[0].toString());
        assertEquals("This is a message", mimeMessage1.getSubject());
        assertEquals(2, mimeMessage1.getReplyTo().length);
        assertEquals(2, mimeMessage1.getRecipients(Message.RecipientType.TO).length);
        assertEquals(1, mimeMessage1.getRecipients(Message.RecipientType.CC).length);
        assertNull(mimeMessage1.getRecipients(Message.RecipientType.BCC));
        
        Multipart multipart1 = (Multipart)mimeMessage1.getContent();
        assertEquals(2, multipart1.getCount());
        assertEquals("body", multipart1.getBodyPart(0).getFileName());
        assertEquals("message.txt", multipart1.getBodyPart(1).getFileName());
        
        String body1 = IOUtils.toString(multipart1.getBodyPart(0).getInputStream(), StandardCharsets.UTF_8);
        assertEquals(message.getPlainTextBody(), body1);
        
        String text1 = IOUtils.toString(multipart1.getBodyPart(1).getInputStream(), StandardCharsets.UTF_8);
        assertEquals("Hello, World!", text1);
        
        // -----------------------------------------------------
        
        MimeMessage mimeMessage2 = message.toMimeMessage();
        try(ByteArrayOutputStream baos2 = new ByteArrayOutputStream()) {
            mimeMessage2.writeTo(baos2);
        }
        
        assertEquals(1, mimeMessage2.getFrom().length);
        assertEquals("sender@jotlmsg.com", mimeMessage2.getFrom()[0].toString());
        assertEquals("This is a message", mimeMessage2.getSubject());
        assertEquals(2, mimeMessage2.getReplyTo().length);
        assertEquals(2, mimeMessage2.getRecipients(Message.RecipientType.TO).length);
        assertEquals(1, mimeMessage2.getRecipients(Message.RecipientType.CC).length);
        assertNull(mimeMessage2.getRecipients(Message.RecipientType.BCC));
        
        Multipart multipart2 = (Multipart)mimeMessage2.getContent();
        assertEquals(2, multipart2.getCount());
        assertEquals("body", multipart2.getBodyPart(0).getFileName());
        assertEquals("message.txt", multipart2.getBodyPart(1).getFileName());
        
        String body2 = IOUtils.toString(multipart2.getBodyPart(0).getInputStream(), StandardCharsets.UTF_8);
        assertEquals(message.getPlainTextBody(), body2);
        
        String text2 = IOUtils.toString(multipart2.getBodyPart(1).getInputStream(), StandardCharsets.UTF_8);
        assertEquals("Hello, World!", text2);
    }
}
