
package ch.astorm.jotlmsg;

import ch.astorm.jotlmsg.OutlookMessageRecipient.Type;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.poi.util.IOUtils;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class OutlookMessageMSGTest {
    
    @Test
    public void testSimpleMessage() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.\nFind some accents: àïâç&@+\"{}$");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.setReplyTo(Arrays.asList("reply1@jotlmsg.com", "reply2@jotlmsg.com"));
        
        testMessage(message);
    }
    
    @Test
    public void testBaseMessage() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is the subject");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");

        testMessage(message);
        testBinary(message, "generated/base-message.msg");
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
        message.addAttachment("message.txt", "text/plain", new ByteArrayInputStream("Hello, World!".getBytes(StandardCharsets.UTF_8)));

        testMessage(message);
    }
    
    @Test
    public void testMessageWithAttachment2() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setPlainTextBody("Hello,\n\nThis is a simple message.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric <djoy@me.com>");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "ctabin@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "ctabin2@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.CC, "cc@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.CC, "cc2@jotlmsg.com", "John");
        message.addRecipient(OutlookMessageRecipient.Type.CC, "cc3@jotlmsg.com");
        message.addRecipient(OutlookMessageRecipient.Type.BCC, "bcc@jotlmsg.com");
        message.addAttachment("message.txt", "text/plain", new ByteArrayInputStream("Hello, World!".getBytes(StandardCharsets.UTF_8)));
        message.addAttachment("message2.txt", "text/plain", new ByteArrayInputStream("Another attachment with content".getBytes(StandardCharsets.UTF_8)));
        message.addAttachment("message3.txt", "text/html", new ByteArrayInputStream("<html><body>Some html page</body></html>".getBytes(StandardCharsets.UTF_8)));

        testMessage(message);
        testBinary(message, "generated/with-attachments-2.msg");
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
        testBinary(message, "generated/without-attachment.msg");
    }

    @Test
    public void testMessageSent() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setPlainTextBody("Hello,\n\nThis is a simple message that has been sent.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");
        message.setSentDate(sdf.parse("28.02.2018"));

        testMessage(message);
    }

    @Test
    public void testHtmlBody() throws Exception {
        String htmlText = """
                          <html>
                            <body>
                                <h1>Title</h1>
                                <p>This is some <strong>bold</strong> and <i>italic</i> text.</p>
                                <p>Here is some <span style=\"color:red\">red</span> text too.</p>
                            </body>
                          </html>
                          """;
        
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setHtmlBody(htmlText.trim());
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        testMessage(message);
    }

    @Test
    public void testHtmlBodyWithInlineAttachment() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setFrom("sender@jotlmsg.com");
        message.setPlainTextBody("Hello,\n\nThis is a simple message that has been sent.\n\n.Bye.");
        message.setHtmlBody("<html><body>Sample body</body></html>");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        final OutlookMessageAttachment inlineAttachment = new OutlookMessageAttachment("picture.png", "image/png", new ByteArrayInputStream(new byte[0]));
        inlineAttachment.setContentId("picture.png@" + UUID.randomUUID());
        message.addAttachment(inlineAttachment);

        testMessage(message);
    }

    private void testBinary(OutlookMessage message, String resPath) throws Exception {
        try(InputStream is = OutlookMessageMSGTest.class.getResourceAsStream(resPath)) {
            OutlookMessage source = new OutlookMessage(is);
            compareMessage(source, message);
        }
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
        compareMessage(source, parsed);
    }
    
    private void compareMessage(OutlookMessage source, OutlookMessage other) throws Exception {
        assertEquals(source.getSubject(), other.getSubject());
        assertEquals(source.getFrom(), other.getFrom());
        assertEquals(source.getPlainTextBody(), other.getPlainTextBody());
        assertEquals(source.getHtmlBody(), other.getHtmlBody());
        assertEquals(source.getAllRecipients().size(), other.getAllRecipients().size());
        assertEquals(source.getAttachments().size(), other.getAttachments().size());
        assertEquals(source.getSentDate(), other.getSentDate());

        if(source.getReplyTo()!=null && other.getReplyTo()!=null) {
            assertEquals(source.getReplyTo().size(), other.getReplyTo().size());
            List<String> srcReplyToRecipients = source.getReplyTo();
            List<String> parsedReplyToRecipients = other.getReplyTo();
            for(int i=0 ; i<srcReplyToRecipients.size() ; ++i) {
                String srcReplyToRecipient = srcReplyToRecipients.get(i);
                String parsedReplyToRecipient = parsedReplyToRecipients.get(i);
                assertEquals(srcReplyToRecipient, parsedReplyToRecipient);
            }
        }
        
        List<OutlookMessageRecipient> srcRecipients = source.getAllRecipients();
        List<OutlookMessageRecipient> parsedRecipients = other.getAllRecipients();
        for(int i=0 ; i<srcRecipients.size() ; ++i) {
            OutlookMessageRecipient srcRecipient = srcRecipients.get(i);
            OutlookMessageRecipient parsedRecipient = parsedRecipients.get(i);
            assertEquals(srcRecipient.getType(), parsedRecipient.getType());
            assertEquals(srcRecipient.getName(), parsedRecipient.getName());
            assertEquals(srcRecipient.getEmail(), parsedRecipient.getEmail());
        }
        
        List<OutlookMessageAttachment> srcAttachments = source.getAttachments();
        List<OutlookMessageAttachment> parsedAttachments = other.getAttachments();
        for(int i=0 ; i<srcAttachments.size() ; ++i) {
            OutlookMessageAttachment srcAttachment = srcAttachments.get(i);
            OutlookMessageAttachment parsedAttachment = parsedAttachments.get(i);
            assertEquals(srcAttachment.getName(), parsedAttachment.getName());
            assertEquals(srcAttachment.getMimeType(), parsedAttachment.getMimeType());
            assertEquals(srcAttachment.getContentId(), parsedAttachment.getContentId());
            byte[] srcData = IOUtils.toByteArray(srcAttachment.getNewInputStream());
            byte[] parData = IOUtils.toByteArray(parsedAttachment.getNewInputStream());
            assertEquals(srcData.length, parData.length);
            assertArrayEquals(srcData, parData);
        }
    }
    
    @Test
    public void testaddManyRecipients() throws Exception {
        OutlookMessage message = new OutlookMessage();
        IntStream.range(0,40).forEach(i -> message.addRecipient(Type.TO, "user" + i + "@xyz.com"));

        message.setSubject("betreff");
        message.setPlainTextBody("content");

        testBinary(message, "generated/many-recipients.msg");
    }
    
    @Test
    public void testaddManyAttachments() throws Exception {
        int count = 40;

        OutlookMessage message = new OutlookMessage();
        IntStream.range(0,count).forEach(i -> message.addAttachment("test"+i+".txt", "text/plain", m -> new ByteArrayInputStream(("this is content "+i).getBytes())));

        message.addRecipient(Type.TO, "john@doe.com");
        message.setSubject("betreff");
        message.setPlainTextBody("content");

        
        testBinary(message, "generated/many-attachments.msg");
    }
    
    @Test
    public void testManyReplyTos() throws Exception {
        OutlookMessage message = new OutlookMessage();
        message.setSubject("This is a message");
        message.setPlainTextBody("Hello,\n\nThis is a simple message with replyto addresses.\n\n.Bye.");
        message.addRecipient(OutlookMessageRecipient.Type.TO, "cedric@jotlmsg.com", "Cédric");

        List<String> replytos = new ArrayList<String>();
        IntStream.range(0,40).forEach(i -> replytos.add("reply"+i+"@jotlmsg.com"));
        message.setReplyTo(replytos);
        
        testMessage(message);
    }
}
