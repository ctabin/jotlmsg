
package ch.astorm.jotlmsg;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class OutlookMessageMIMETest {
    
    @Test
    public void testBasicGeneration() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        assertThrows(MessagingException.class, () -> message.toMimeMessage());

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
        assertNull(multipart1.getBodyPart(0).getFileName());
        
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
        assertNull(multipart1.getBodyPart(0).getFileName());
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
        assertNull(multipart2.getBodyPart(0).getFileName());
        assertEquals("message.txt", multipart2.getBodyPart(1).getFileName());
        
        String body2 = IOUtils.toString(multipart2.getBodyPart(0).getInputStream(), StandardCharsets.UTF_8);
        assertEquals(message.getPlainTextBody(), body2);
        
        String text2 = IOUtils.toString(multipart2.getBodyPart(1).getInputStream(), StandardCharsets.UTF_8);
        assertEquals("Hello, World!", text2);
    }

    @Test
    public void testInvalidAttachment() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setReplyTo(Arrays.asList("reply1@jotlmsg.com", "reply2@jotlmsg.com"));
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.addAttachment("message.txt", "text/plain", a -> null);

        File temporaryFile = new File("tmp");
        assertThrows(IllegalStateException.class, () -> message.writeTo(temporaryFile));
        temporaryFile.delete();

        assertThrows(IllegalStateException.class, () -> message.toMimeMessage());
    }

    @Test
    public void testClosedStream() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setReplyTo(Arrays.asList("reply1@jotlmsg.com", "reply2@jotlmsg.com"));
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        CheckableInputStream cis = new CheckableInputStream();
        message.addAttachment("message.txt", "text/plain", a -> cis);

        message.toMimeMessage();
        assertTrue(cis.closed);

        assertThrows(IllegalStateException.class, () -> message.toMimeMessage());
    }

    @Test
    public void testRepeteableStream() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setReplyTo(Arrays.asList("reply1@jotlmsg.com", "reply2@jotlmsg.com"));
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        CheckableInputStream cis = new CheckableInputStream();
        message.addAttachment("message.txt", "text/plain", cis);

        message.toMimeMessage();
        assertTrue(cis.closed);

        message.toMimeMessage();
    }

    @Test
    public void plainAndHtmlMail_shouldUseMultiPartAlternative() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setReplyTo(Arrays.asList("reply1@jotlmsg.com", "reply2@jotlmsg.com"));
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.setHtmlBody("<html><body>Simple body</body></html>");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        final MimeMessage mimeMessage = message.toMimeMessage();
        assertTrue(mimeMessage.getDataHandler().getContentType().startsWith("multipart/mixed"));
        final Object content = mimeMessage.getContent();
        assertInstanceOf(MimeMultipart.class, content);
        final MimeMultipart mimeMultipart = (MimeMultipart) content;
        assertEquals(1, mimeMultipart.getCount());
        final BodyPart firstBodyPart = mimeMultipart.getBodyPart(0);
        assertTrue(firstBodyPart.getDataHandler().getContentType().startsWith("multipart/alternative"));
    }

    private static class CheckableInputStream extends InputStream {
        private boolean closed = false;

        @Override
        public int read() throws IOException {
            if(closed) { throw new IllegalStateException("stream is closed"); }
            return -1;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
