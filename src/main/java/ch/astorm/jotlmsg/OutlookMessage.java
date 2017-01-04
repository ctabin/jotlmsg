
package ch.astorm.jotlmsg;

import ch.astorm.jotlmsg.OutlookMessageRecipient.Type;
import ch.astorm.jotlmsg.io.MessagePropertiesChunk;
import ch.astorm.jotlmsg.io.PropertiesChunk;
import static ch.astorm.jotlmsg.io.PropertiesChunk.FLAG_READABLE;
import static ch.astorm.jotlmsg.io.PropertiesChunk.FLAG_WRITEABLE;
import ch.astorm.jotlmsg.io.StoragePropertiesChunk;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.datatypes.AttachmentChunks;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.NameIdChunks;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.RecipientChunks;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.StringUtil;

/**
 * Represents an Outlook message.
 * <p>This class is meant to be a very simple and easy-to-use API to read and create
 * {@code .msg} files for Microsoft Outlook.</p>
 * 
 * @author Cedric Tabin
 */
public class OutlookMessage {
    private String subject;
    private String plainTextBody;
    private String from;
    private List<String> replyTo;
    
    private final Map<Type, List<OutlookMessageRecipient>> recipients = new EnumMap<>(Type.class);
    private final List<OutlookMessageAttachment> attachments = new ArrayList<>(8);

    /**
     * Creates a new empty message.
     */
    public OutlookMessage() {}
    
    /**
     * Creates a new message with the data of the specified {@code mapiMessageInputStream}.
     * 
     * @param mapiMessageInputStream The source message data.
     * @throws IOException If an I/O error occurs.
     */
    public OutlookMessage(InputStream mapiMessageInputStream) throws IOException {
        this(new MAPIMessage(mapiMessageInputStream));
    }
    
    /**
     * Creates a new message with the data of the specified {@code mapiMessageFile}.
     * 
     * @param mapiMessageFile The source message data.
     * @throws IOException If an I/O error occurs.
     */
    public OutlookMessage(File mapiMessageFile) throws IOException {
        this(new MAPIMessage(mapiMessageFile));
    }
    
    /**
     * Creates a new message with the data of the specified {@code mapiMessage}.
     * All the data will be copied from the source message and the latter can be then discarded.
     * 
     * @param mapiMessage The source message data.
     */
    public OutlookMessage(MAPIMessage mapiMessage) {
        parseMAPIMessage(mapiMessage);
    }
    
    /**
     * Defines the subject of the message.
     * This value may be null.
     */
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    /**
     * Defines the plain text body of the message. Currently, there is no way to define
     * a formatted message (HTML/RTF) for technical reasons (RTF compression and {@code PidTagBodyHtml}
     * not supported by Outlook).
     * This value may be null.
     */
    public String getPlainTextBody() { return plainTextBody; }
    public void setPlainTextBody(String plainTextBody) { this.plainTextBody = plainTextBody; }

    /**
     * Defines the From email address from which a message has been sent.
     * This value may be null.
     */
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    /**
     * Defines the addresses use when the user hits the 'reply' button.
     * This value may be null.
     */
    public List<String> getReplyTo() { return replyTo; }
    public void setReplyTo(List<String> replyTo) { this.replyTo = replyTo; }
    
    /**
     * Returns all the recipients of the specified type. If there is none, then an
     * empty list will be returned.
     * 
     * @param type The recipients type.
     * @return An immutable list with all the recipients of the given type.
     */
    public List<OutlookMessageRecipient> getRecipients(Type type) { return Collections.unmodifiableList(recipients.getOrDefault(type, new ArrayList<>(0))); }
    
    /**
     * Returns all the recipients of this message. If there is no recipient, an empty
     * list will be returned.
     * 
     * @return A new list with all the recipients.
     */
    public List<OutlookMessageRecipient> getAllRecipients() {
        List<OutlookMessageRecipient> allRecipients = new ArrayList<>(16);
        recipients.forEach((k,v) -> allRecipients.addAll(v));
        return allRecipients;
    }
    
    /**
     * Creates and add a new {@code OutlookMessageRecipient} to this message.
     * 
     * @param type The type.
     * @param email The email.
     * @return The created recipient.
     */
    public OutlookMessageRecipient addRecipient(Type type, String email) { return addRecipient(type, email, null); }
    
    /**
     * Creates and add a new {@code OutlookMessageRecipient} to this message.
     * 
     * @param type The type.
     * @param email The email.
     * @param name The name or null.
     * @return The created recipient.
     */
    public OutlookMessageRecipient addRecipient(Type type, String email, String name) {  
        OutlookMessageRecipient recipient = new OutlookMessageRecipient(type, email, name);
        addRecipient(recipient);
        return recipient;
    }
    
    /**
     * Add the specified {@code recipient} to this message.
     * 
     * @param recipient The recipient to add.
     */
    public void addRecipient(OutlookMessageRecipient recipient) { 
        if(recipient==null) { throw new IllegalArgumentException("recipient is not defined"); }
        List<OutlookMessageRecipient> typeRecipients = recipients.get(recipient.getType());
        if(typeRecipients==null) {
            typeRecipients = new ArrayList<>(31);
            recipients.put(recipient.getType(), typeRecipients);
        }
        typeRecipients.add(recipient);
    }
    
    /**
     * Removes the specified {@code recipient} from this message.
     * 
     * @param recipient The recipient to remove.
     */
    public void removeRecipient(OutlookMessageRecipient recipient) {
        List<OutlookMessageRecipient> typeRecipients = recipients.get(recipient.getType());
        if(typeRecipients!=null) { typeRecipients.remove(recipient); }
    }
    
    /**
     * Removes all the recipients of the given type.
     * 
     * @param type The type of recipients to remove.
     */
    public void removeAllRecipients(Type type) {
        recipients.remove(type);
    }
    
    /**
     * Returns the attachments of this message. This list can be directly modified.
     * 
     * @return The attachments.
     */
    public List<OutlookMessageAttachment> getAttachments() {
        return attachments;
    }
    
    /**
     * Add a new attachment to this message. It is possible to associate the {@code InputStream}
     * data after the attachment creation.
     * 
     * @param name The name.
     * @param mimeType The MIME type.
     * @param input The input data.
     * @return The created attachment.
     */
    public OutlookMessageAttachment addAttachment(String name, String mimeType, InputStream input) {
        OutlookMessageAttachment attachment = new OutlookMessageAttachment(name, mimeType, input);
        addAttachment(attachment);
        return attachment;
    }
    
    /**
     * Add a new attachment to this message. 
     * 
     * @param attachment The attachment.
     */
    public void addAttachment(OutlookMessageAttachment attachment) {
        if(attachment==null) { throw new IllegalArgumentException("attachment is not defined"); }
        attachments.add(attachment);
    }
    
    /**
     * Removes the specified attachment from this message.
     * 
     * @param attachment The attachment to remove.
     */
    public void removeAttachment(OutlookMessageAttachment attachment) {
        attachments.remove(attachment);
    }
    
    /**
     * Removes all the attachments from this message.
     */
    public void removeAllAttachments() {
        attachments.clear();
    }
    
    /**
     * Creates a new {@code MimeMessage} from this {@code OutlookMessage}.
     * A new {@link Session} will be created with an empty {@code Properties} instance.
     * 
     * @return A new {@code MimeMessage} instance.
     * @see #toMimeMessage(java.util.Properties)
     */
    public MimeMessage toMimeMessage() throws IOException, MessagingException {
        return toMimeMessage(new Properties());
    }
    
    /**
     * Creates a new {@code MimeMessage} from this {@code OutlookMessage}.
     * A new {@link Session} will be created with the specified {@code sessionProps}.
     * 
     * @param sessionProps The {@code Session} properties.
     * @return A new {@code MimeMessage} instance.
     * @see #toMimeMessage(javax.mail.Session) 
     */
    public MimeMessage toMimeMessage(Properties sessionProps) throws IOException, MessagingException {
        Session session = Session.getInstance(sessionProps);
        return toMimeMessage(session);
    }
    
    /**
     * Creates a new {@code MimeMessage} from this {@code OutlookMessage}.
     * This method will generate a multipart/mixed {@code MimeMessage}, with the first
     * part being the message body (named 'body').
     * 
     * @param session The {@code Session} to use for message creation.
     * @return A new {@code MimeMessage} instance.
     */
    public MimeMessage toMimeMessage(Session session) throws IOException, MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setSentDate(new Date());
        
        String subject = getSubject();
        if(subject!=null) { message.setSubject(subject); }
        
        String from = getFrom();
        if(from!=null) { message.setFrom(new InternetAddress(from)); }
        
        List<String> replyTo = getReplyTo();
        if(replyTo!=null) {
            List<Address> replyAddresses = new ArrayList<>(replyTo.size());
            for(String replyToEmail : replyTo) {
                if(replyToEmail!=null) { replyAddresses.add(new InternetAddress(replyToEmail)); }
            }
            message.setReplyTo(replyAddresses.toArray(new Address[replyAddresses.size()]));
        }
        
        for(OutlookMessageRecipient recipient : getAllRecipients()) {
            Address address = recipient.getAddress();
            if(address!=null) { message.addRecipient(recipient.getType().getRecipientType(), address); }
        }
        
        MimeMultipart multipart = new MimeMultipart();
        
        String plainText = getPlainTextBody();
        if(plainText==null) { throw new MessagingException("missing body"); }
        MimeBodyPart body = new MimeBodyPart();
        body.setFileName("body");
        body.setText(getPlainTextBody(), "UTF-8", "plain");
        multipart.addBodyPart(body);
        
        for(OutlookMessageAttachment attachment : getAttachments()) {
            InputStream inputStream = attachment.getNewInputStream();
            if(inputStream!=null) { 
                MimeBodyPart part = new MimeBodyPart();
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(inputStream, attachment.getMimeType())));
                part.setFileName(attachment.getName());
                multipart.addBodyPart(part);
            }
        }
        
        message.setContent(multipart);
        return message;
    }
    
    /**
     * Writes the content of this message to the specified {@code file}. The created
     * file will be in format {@code .msg} that can be open by Microsoft Outlook.
     * 
     * @param file The {@code .msg} file to create.
     * @throws IOException If an I/O error occurs.
     */
    public void writeTo(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        writeTo(fos);
        fos.close();
    }
    
    /**
     * Writes the content of this message to the specified {@code outputStream}. The
     * bytes written represent a {@code .msg} file that can be open by Microsoft Outlook.
     * The {@code outputStream} will remain open.
     * 
     * @param outputStream The stream to write to.
     * @throws IOException If an I/O error occurs.
     */
    public void writeTo(OutputStream outputStream) throws IOException {
        NPOIFSFileSystem fs = new NPOIFSFileSystem();
        
        List<OutlookMessageRecipient> recipients = getAllRecipients();
        List<OutlookMessageAttachment> attachments = getAttachments();
        String body = getPlainTextBody();
        String subject = getSubject();
        String from = getFrom();
        
        //creates the basic structure (page 17, point 2.2.3)
        DirectoryEntry nameid = fs.createDirectory(NameIdChunks.NAME);
        nameid.createDocument(PropertiesChunk.PREFIX+"00020102", new ByteArrayInputStream(new byte[0])); //GUID Stream
        nameid.createDocument(PropertiesChunk.PREFIX+"00030102", new ByteArrayInputStream(new byte[0])); //Entry Stream (mandatory, otherwise Outlook crashes)
        nameid.createDocument(PropertiesChunk.PREFIX+"00040102", new ByteArrayInputStream(new byte[0])); //String Stream
        
        //creates the top-level structure of data
        MessagePropertiesChunk topLevelChunk = new MessagePropertiesChunk();
        topLevelChunk.setAttachmentCount(attachments.size());
        topLevelChunk.setRecipientCount(recipients.size());
        topLevelChunk.setNextAttachmentId(attachments.isEmpty() ? 0 : 1);
        topLevelChunk.setNextRecipientId(recipients.isEmpty() ? 0 : 1);
        topLevelChunk.setConstainsStorageAttachment(!attachments.isEmpty() || !recipients.isEmpty());
        
        topLevelChunk.setProperty(new PropertyValue(MAPIProperty.STORE_SUPPORT_MASK, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(0x00040000).array())); //all the strings will be in unicode
        if(subject!=null) { topLevelChunk.setProperty(new PropertyValue(MAPIProperty.SUBJECT, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(subject))); }
        if(body!=null) { topLevelChunk.setProperty(new PropertyValue(MAPIProperty.BODY, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(body))); }
        if(from!=null) { 
            topLevelChunk.setProperty(new PropertyValue(MAPIProperty.SENDER_EMAIL_ADDRESS, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(from))); 
            topLevelChunk.setProperty(new PropertyValue(MAPIProperty.SENDER_NAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(from))); 
        }
        
        topLevelChunk.writeTo(fs.getRoot());
        
        //creates the recipients
        int recipientCounter = 0;
        for(OutlookMessageRecipient recipient : recipients) {
            if(recipientCounter>=2048) { throw new RuntimeException("too many recipients (max=2048)"); } //limitation, see page 15, point 2.2.1
            
            String name = recipient.getName();
            String email = recipient.getEmail();
            
            StoragePropertiesChunk recipStorage = new StoragePropertiesChunk();
            if(name!=null) { 
                recipStorage.setProperty(new PropertyValue(MAPIProperty.DISPLAY_NAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
                recipStorage.setProperty(new PropertyValue(MAPIProperty.RECIPIENT_DISPLAY_NAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
            }
            if(email!=null) { recipStorage.setProperty(new PropertyValue(MAPIProperty.EMAIL_ADDRESS, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(email))); }
           
            String rid = ""+recipientCounter;
            while(rid.length()<8) { rid = "0"+rid; }
            DirectoryEntry recip = fs.createDirectory(RecipientChunks.PREFIX+"#"+rid); //page 15, point 2.2.1
            recipStorage.writeTo(recip);
            
            ++recipientCounter;
        }
        
        //creates the attachments
        int attachmentCounter = 0;
        for(OutlookMessageAttachment attachment : attachments) {
            if(attachmentCounter>=2048) { throw new RuntimeException("too many attachments (max=2048)"); } //limitation, see page 15, point 2.2.2
            
            InputStream is = attachment.getNewInputStream();
            String name = attachment.getName();
            String mimeName = attachment.getMimeType();
            byte[] data = IOUtils.toByteArray(is);
            is.close();
            
            StoragePropertiesChunk attachStorage = new StoragePropertiesChunk();
            if(name!=null) { 
                attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_FILENAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
                attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_LONG_FILENAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
            }
            if(mimeName!=null) { attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_MIME_TAG, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); }
            attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_DATA, FLAG_READABLE | FLAG_WRITEABLE, data));
            
            String rid = ""+attachmentCounter;
            while(rid.length()<8) { rid = "0"+rid; }
            DirectoryEntry recip = fs.createDirectory(AttachmentChunks.PREFIX+"#"+rid); //page 15, point 2.2.1
            attachStorage.writeTo(recip);
            
            ++attachmentCounter;
        }
        
        fs.writeFilesystem(outputStream);
        fs.close();
    }
    
    private void parseMAPIMessage(MAPIMessage mapiMessage) {
        silent(()-> parseFrom(mapiMessage));
        silent(()-> parseReplyTo(mapiMessage));
        silent(()-> parseSubject(mapiMessage));
        silent(()-> parseTextBody(mapiMessage));
        silent(()-> parseRecipients(mapiMessage));
        silent(()-> parseAttachments(mapiMessage));
    }
    
    /**
     * Parses the From field from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     */
    protected void parseFrom(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        //TODO
        this.from = mapiMessage.getDisplayFrom();
        if(from!=null) { this.from = from.trim(); }
        if(from!=null && from.isEmpty()) { this.from = null; }
    }
    
    /**
     * Parses the Reply-To field from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     */
    protected void parseReplyTo(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        //TODO
    }
    
    /**
     * Parses the Subject field from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     */
    protected void parseSubject(MAPIMessage mapiMessage) throws ChunkNotFoundException { 
        this.subject = mapiMessage.getSubject();
        if(subject!=null) { this.subject = subject.trim(); }
        if(subject!=null && subject.isEmpty()) { this.subject = null; }
    }
    
    /**
     * Parses the text body from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     */
    protected void parseTextBody(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        this.plainTextBody = mapiMessage.getTextBody();
        if(plainTextBody!=null) { this.plainTextBody = plainTextBody.trim(); }
        if(plainTextBody!=null && plainTextBody.isEmpty()) { this.plainTextBody = null; }
    }
    
    /**
     * Parses the recipients from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     */
    protected void parseRecipients(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        RecipientChunks[] recipientChunks = mapiMessage.getRecipientDetailsChunks();
        for(RecipientChunks recipientChunk : recipientChunks) {
            String name = recipientChunk.getRecipientName();
            String email = recipientChunk.getRecipientEmailAddress();
            
            if(name!=null && email!=null && name.equals(email)) {
                name = null;
            }
            
            Type type = Type.TO;
            List<PropertyValue> values = recipientChunk.getProperties().get(MAPIProperty.RECIPIENT_TYPE);
            if(values!=null && !values.isEmpty()) { 
                int value = (int)values.get(0).getValue();
                if(value==1) { type = Type.TO; }
                else if(value==2) { type = Type.CC; }
                else if(value==3) { type = Type.BCC; }
            }
            
            addRecipient(type, email, name);
        }
    }
    
    /**
     * Parses the attachments from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     */
    protected void parseAttachments(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        AttachmentChunks[] attachmentChunks = mapiMessage.getAttachmentFiles();
        for(AttachmentChunks attachmentChunk : attachmentChunks) {
            String name = attachmentChunk.attachLongFileName!=null ? attachmentChunk.attachLongFileName.getValue() :
                          attachmentChunk.attachFileName!=null ? attachmentChunk.attachFileName.getValue() :
                                                                 attachmentChunk.getPOIFSName();
            InputStream data = attachmentChunk.attachData!=null ? new ByteArrayInputStream(attachmentChunk.attachData.getValue()) : null;
            String mimeType = attachmentChunk.attachMimeTag!=null ? attachmentChunk.attachMimeTag.getValue() : null;
            addAttachment(name, mimeType, data);
        }
    }
    
    private boolean silent(SilentCallFailure call) {
        try { call.invoke(); }
        catch(ChunkNotFoundException ignored) { return false; }
        return true;
    }
    
    private static interface SilentCallFailure {
        void invoke() throws ChunkNotFoundException;
    }
}
