/*
  jotlmsg API
  Copyright (C) 2017 Cédric Tabin

  This file is part of jotlmsg, a library to easily manipulate and generate
  Outlook msg files.
  The author can be contacted on http://www.astorm.ch/blog/index.php?contact

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:
  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in
     the documentation and/or other materials provided with the
     distribution.
  3. The names of the authors may not be used to endorse or promote
     products derived from this software without specific prior
     written permission.
 
  THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS
  OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
  IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package ch.astorm.jotlmsg;

import ch.astorm.jotlmsg.OutlookMessageAttachment.InputStreamCreator;
import ch.astorm.jotlmsg.OutlookMessageAttachment.MemoryInputStreamCreator;
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
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.apache.poi.hsmf.datatypes.ByteChunk;
import org.apache.poi.hsmf.datatypes.Chunk;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.NameIdChunks;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.RecipientChunks;
import org.apache.poi.hsmf.datatypes.StringChunk;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.StringUtil;

/**
 * Represents an Outlook message.
 * <p>This class is meant to be a very simple and easy-to-use API to read and create
 * {@code .msg} files for Microsoft Outlook. It is also compatible with the standard
 * {@code javax.mail} package, by the usage of the {@link #toMimeMessage()} method.</p>
 * <p>This implementation is capable of generating {@code .msg} files that can be open by
 * Microsoft Outlook, so it behaves like if the user has created a new email. However it's
 * purpose is not to make a full-integration of Microsoft Outlook with all the features it
 * has (such as calendars, appointments, tasks, ...). Those features may be covered in future
 * developments.</p>
 * <p>The Microsoft specification {@code MS-OXCMSG} can be found in the {@code ms-specifications}
 * folder of jotlmsg, or can be downloaded <a href="https://msdn.microsoft.com/en-us/library/cc463900%28v=exchg.80%29.aspx">here</a>.
 * The detailed list of properties {@code MS-OXPROPS} is also available in the {@code ms-specifications}
 * folder or <a href="https://msdn.microsoft.com/en-us/library/cc433490%28v=exchg.80%29.aspx">here</a>.</p>
 * 
 * @author Cedric Tabin
 */
public class OutlookMessage {
    private String subject;
    private String plainTextBody;
    private String from;
    private List<String> replyTo;
    private Date sentDate;
    
    private final Map<Type, List<OutlookMessageRecipient>> recipients = new EnumMap<>(Type.class);
    private final List<OutlookMessageAttachment> attachments = new ArrayList<>(8);

    /**
     * Format of a MIME date that can be used as pattern in a {@link SimpleDateFormat}.
     */
    public static final String MIME_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z (z)";

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
     * Defines the {@code From} email address from which a message has been sent.
     * This value may be null.
     */
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    /**
     * Defines the addresses use when the user hits the 'reply' button.
     * Currently, this value is only used to create a {@link MimeMessage} but isn't
     * used for {@code .msg} generation.
     * This value may be null.
     * 
     * @see #toMimeMessage()
     */
    public List<String> getReplyTo() { return replyTo; }
    public void setReplyTo(List<String> replyTo) { this.replyTo = replyTo; }

    /**
     * Defines the sent date of the message. If this value is not defined, then it
     * means the message is considered as unsent.
     * <p>However, it is possible that this value is set when reading a message that
     * was created by Outlook and may then contains its creation date.</p>
     */
    public Date getSentDate() { return sentDate; }
    public void setSentDate(Date d) { this.sentDate = d; }

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
     * Removes all the recipients.
     */
    public void removeAllRecipients() {
        recipients.clear();
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
     * Add a new attachment to this message.
     * <p>The data of the {@code InputStream} will be loaded into memory (see {@link MemoryInputStreamCreator}).
     * If you don't expect to invoke {@link #writeTo(java.io.OutputStream) writeTo()} or {@link #toMimeMessage() toMimeMessage()} multiple times,
     * then consider using the {@link #addAttachment(java.lang.String, java.lang.String, ch.astorm.jotlmsg.OutlookMessageAttachment.InputStreamCreator) other}
     * method, which uses a {@link InputStreamCreator}.</p>
     * <p>The <a href="https://fr.wikipedia.org/wiki/Type_de_m%C3%A9dias">MIME</a> type must be set
     * accordingly to the {@code input}.</p>
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
     * Add a new attachment to this message. The {@code InputStreamCreator} can be
     * set to the attachment later.
     * <p>To use this method with a single-usage {@code InputStream}:</p>
     * <pre>message.addAttachment("myAttachment", "text/plain", a -&gt; myInputStream);</pre>
     * <p>The <a href="https://fr.wikipedia.org/wiki/Type_de_m%C3%A9dias">MIME</a> type must be set
     * accordingly to the {@code input}.</p>
     *
     * @param name The name.
     * @param mimeType The MIME type.
     * @param inputStreamCreator The {@code InputStream} creator or null.
     * @return The created attachment.
     */
    public OutlookMessageAttachment addAttachment(String name, String mimeType, InputStreamCreator inputStreamCreator) {
        OutlookMessageAttachment attachment = new OutlookMessageAttachment(name, mimeType, inputStreamCreator);
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
        message.setSentDate(sentDate);
        
        String subject = getSubject();
        if(subject!=null) { message.setSubject(subject); }
        
        String from = extractEmail(getFrom());
        if(from!=null) { message.setFrom(new InternetAddress(from)); }
        
        List<String> replyTo = getReplyTo();
        if(replyTo!=null) {
            List<Address> replyAddresses = new ArrayList<>(replyTo.size());
            for(String replyToEmail : replyTo) {
                String replyToEmailExtracted = extractEmail(replyToEmail);
                if(replyToEmailExtracted!=null) { replyAddresses.add(new InternetAddress(replyToEmailExtracted)); }
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
        body.setText(getPlainTextBody(), StandardCharsets.UTF_8.name(), "plain");
        multipart.addBodyPart(body);
        
        for(OutlookMessageAttachment attachment : getAttachments()) {
            String name = attachment.getName();
            String mimeType = attachment.getMimeType();
            byte[] data = readAttachement(attachment);

            MimeBodyPart part = new MimeBodyPart();
            part.setDataHandler(new DataHandler(new ByteArrayDataSource(data, mimeType)));
            part.setFileName(name);
            multipart.addBodyPart(part);
        }
        
        message.setContent(multipart);
        return message;
    }

    private static final Pattern MIXED_MAIL = Pattern.compile("[^\\s<>,/]+@[^\\s<>,/]+");

    /**
     * Extracts the mail from a mixed mail string, for instance "John Doe &lt;john.doe@hotmail.com&gt;".
     * <p>This method uses a simple pattern to get the first string which looks like an email,
     * eg there is an @ with at least one char on each side. Spaces are considered as separators.</p>
     * <p>If there are multiple mails in {@code mixedMailStr}, only the first one will be returned.</p>
     * <p>This method does not check at all if the returned mail is valid.</p>
     *
     * @param mixedMailStr A string that contains (potentially) an email address.
     * @return The first string looking like an email or null.
     */
    public static String extractEmail(String mixedMailStr) {
        if(mixedMailStr==null || mixedMailStr.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = MIXED_MAIL.matcher(mixedMailStr);
        return matcher.find() ? matcher.group() : null;
    }

    /**
     * Writes the content of this message to the specified {@code file}. The created
     * file will be in format {@code .msg} that can be open by Microsoft Outlook.
     * 
     * @param file The {@code .msg} file to create.
     * @throws IOException If an I/O error occurs.
     */
    public void writeTo(File file) throws IOException {
        try(FileOutputStream fos = new FileOutputStream(file)) {
            writeTo(fos);
        }
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
        POIFSFileSystem fs = new POIFSFileSystem();
        
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
        topLevelChunk.setNextAttachmentId(attachments.size()); //actually indicates the next free id !
        topLevelChunk.setNextRecipientId(recipients.size()); //actually indicates the next free id !

        //constants values can be found here: https://msdn.microsoft.com/en-us/library/ee219881(v=exchg.80).aspx
        topLevelChunk.setProperty(new PropertyValue(MAPIProperty.STORE_SUPPORT_MASK, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(0x00040000).array())); //all the strings will be in unicode
        topLevelChunk.setProperty(new PropertyValue(MAPIProperty.MESSAGE_CLASS, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE("IPM.Note"))); //outlook message
        topLevelChunk.setProperty(new PropertyValue(MAPIProperty.HASATTACH, FLAG_READABLE | FLAG_WRITEABLE, attachments.isEmpty() ? new byte[]{0} : new byte[]{1}));
        if(sentDate==null) { topLevelChunk.setProperty(new PropertyValue(MAPIProperty.MESSAGE_FLAGS, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(8).array())); } //mfUnsent - https://msdn.microsoft.com/en-us/library/ee160304(v=exchg.80).aspx
        else {
            SimpleDateFormat mdf = new SimpleDateFormat(MIME_DATE_FORMAT);
            topLevelChunk.setProperty(new PropertyValue(MAPIProperty.MESSAGE_FLAGS, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(2).array())); //mfUnmodified
            topLevelChunk.setProperty(new PropertyValue(MAPIProperty.CLIENT_SUBMIT_TIME, FLAG_READABLE | FLAG_WRITEABLE, dateToBytes(sentDate)));
            topLevelChunk.setProperty(new PropertyValue(MAPIProperty.TRANSPORT_MESSAGE_HEADERS, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE("Date: "+mdf.format(sentDate))));
        }
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
            Type type = recipient.getType();
            
            int rt = type==Type.TO ? 1 :
                     type==Type.CC ? 2 :
                                     3 ;
            
            StoragePropertiesChunk recipStorage = new StoragePropertiesChunk();
            recipStorage.setProperty(new PropertyValue(MAPIProperty.OBJECT_TYPE, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(6).array())); //MAPI_MAILUSER
            recipStorage.setProperty(new PropertyValue(MAPIProperty.DISPLAY_TYPE, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(0).array())); //DT_MAILUSER
            recipStorage.setProperty(new PropertyValue(MAPIProperty.RECIPIENT_TYPE, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(rt).array())); 
            recipStorage.setProperty(new PropertyValue(MAPIProperty.ROWID, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(recipientCounter).array())); 
            if(name!=null) { 
                recipStorage.setProperty(new PropertyValue(MAPIProperty.DISPLAY_NAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
                recipStorage.setProperty(new PropertyValue(MAPIProperty.RECIPIENT_DISPLAY_NAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
            }
            if(email!=null) { 
                recipStorage.setProperty(new PropertyValue(MAPIProperty.EMAIL_ADDRESS, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(email))); 
                if(name==null) {
                    recipStorage.setProperty(new PropertyValue(MAPIProperty.DISPLAY_NAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(email))); 
                    recipStorage.setProperty(new PropertyValue(MAPIProperty.RECIPIENT_DISPLAY_NAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(email))); 
                }
            }
           
            String rid = ""+Integer.toHexString(recipientCounter);
            while(rid.length()<8) { rid = "0"+rid; }
            DirectoryEntry recip = fs.createDirectory(RecipientChunks.PREFIX+rid); //page 15, point 2.2.1
            recipStorage.writeTo(recip);
            
            ++recipientCounter;
        }
        
        //creates the attachments
        int attachmentCounter = 0;
        for(OutlookMessageAttachment attachment : attachments) {
            if(attachmentCounter>=2048) { throw new RuntimeException("too many attachments (max=2048)"); } //limitation, see page 15, point 2.2.2

            String name = attachment.getName();
            String mimeType = attachment.getMimeType();
            byte[] data = readAttachement(attachment);
            
            StoragePropertiesChunk attachStorage = new StoragePropertiesChunk();
            attachStorage.setProperty(new PropertyValue(MAPIProperty.OBJECT_TYPE, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(7).array())); //MAPI_ATTACH
            if(name!=null) { 
                attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_FILENAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
                attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_LONG_FILENAME, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(name))); 
            }
            if(mimeType!=null) { attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_MIME_TAG, FLAG_READABLE | FLAG_WRITEABLE, StringUtil.getToUnicodeLE(mimeType))); }
            attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_NUM, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(attachmentCounter).array()));
            attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_METHOD, FLAG_READABLE | FLAG_WRITEABLE, ByteBuffer.allocate(4).putInt(1).array())); //ATTACH_BY_VALUE
            attachStorage.setProperty(new PropertyValue(MAPIProperty.ATTACH_DATA, FLAG_READABLE | FLAG_WRITEABLE, data));
            
            String rid = ""+Integer.toHexString(attachmentCounter);
            while(rid.length()<8) { rid = "0"+rid; }
            DirectoryEntry recip = fs.createDirectory(AttachmentChunks.PREFIX+rid); //page 15, point 2.2.1
            attachStorage.writeTo(recip);
            
            ++attachmentCounter;
        }
        
        fs.writeFilesystem(outputStream);
        fs.close();
    }

    private byte[] readAttachement(OutlookMessageAttachment attachment) throws IOException {
        try(InputStream is = attachment.getNewInputStream()) {
            if(is==null) { throw new IllegalStateException("null inputstream for attachement "+attachment.getName()+" ("+attachment.getMimeType()+")"); }
            return IOUtils.toByteArray(is);
        }
    }

    //extracted from org.apache.poi.hsmf.datatypes.TimePropertyValue
    private final long OFFSET = 1000L * 60L * 60L * 24L * (365L * 369L + 89L);
    private byte[] dateToBytes(Date date) {
        return ByteBuffer.allocate(8).putLong((date.getTime()+OFFSET)*10*1000).array();
    }
    
    private void parseMAPIMessage(MAPIMessage mapiMessage) {
        silent(() -> parseHeaders(mapiMessage));
        silent(() -> parseFrom(mapiMessage));
        silent(() -> parseReplyTo(mapiMessage));
        silent(() -> parseSubject(mapiMessage));
        silent(() -> parseTextBody(mapiMessage));
        silent(() -> parseRecipients(mapiMessage));
        silent(() -> parseAttachments(mapiMessage));
    }
    
    /**
     * Parses the From field from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     * 
     * @param mapiMessage The message to parse.
     * @throws ChunkNotFoundException If some data is not find in the {@code mapiMessage}.
     */
    protected void parseFrom(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        this.from = mapiMessage.getDisplayFrom();
        if(from!=null) { this.from = from.trim(); }
        if(from!=null && from.isEmpty()) { this.from = null; }
    }
    
    /**
     * Parses the Reply-To field from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     * Currently, this method does nothing.
     * 
     * @param mapiMessage The message to parse.
     * @throws ChunkNotFoundException If some data is not found in the {@code mapiMessage}.
     */
    protected void parseReplyTo(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        //TODO
    }
    
    /**
     * Parses the Subject field from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     * 
     * @param mapiMessage The message to parse.
     * @throws ChunkNotFoundException If some data is not found in the {@code mapiMessage}.
     */
    protected void parseSubject(MAPIMessage mapiMessage) throws ChunkNotFoundException { 
        this.subject = mapiMessage.getSubject();
        if(subject!=null) { this.subject = subject.trim(); }
        if(subject!=null && subject.isEmpty()) { this.subject = null; }
    }
    
    /**
     * Parses the text body from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     * 
     * @param mapiMessage The message to parse.
     * @throws ChunkNotFoundException If some data is not found in the {@code mapiMessage}.
     */
    protected void parseTextBody(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        this.plainTextBody = mapiMessage.getTextBody();
        if(plainTextBody!=null) { this.plainTextBody = plainTextBody.trim(); }
        if(plainTextBody!=null && plainTextBody.isEmpty()) { this.plainTextBody = null; }
    }
    
    /**
     * Parses the recipients from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     * 
     * @param mapiMessage The message to parse.
     * @throws ChunkNotFoundException If some data is not found in the {@code mapiMessage}.
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
     * Parses the MIME headers of the {@code mapiMessage}. By default, this message will
     * only check for the {@code Date} header.
     * 
     * @param mapiMessage The message.
     * @throws ChunkNotFoundException If some data is not found in the {@code mapiMessage}.
     */
    protected void parseHeaders(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        List<Chunk> headerChunks = mapiMessage.getMainChunks().getAll().get(MAPIProperty.TRANSPORT_MESSAGE_HEADERS);
        if(headerChunks!=null && !headerChunks.isEmpty()) {
            for(Chunk chunk : headerChunks) {
                if(chunk instanceof StringChunk) {
                    StringChunk sc = (StringChunk)chunk;
                    String value = sc.getValue();
                    int dateIdx = value.indexOf("Date:");
                    if(dateIdx>=0) {
                        int line = value.indexOf('\n', dateIdx+5);
                        int semiColon = value.indexOf(';', dateIdx+5);
                        int limit = line>=0 && semiColon>=0 ? Math.min(line, semiColon) :
                                    line>=0 ? line : semiColon;
                        String dateStr = value.substring(dateIdx+5, limit>=0 ? limit : value.length()).trim();
                        SimpleDateFormat mdf = new SimpleDateFormat(MIME_DATE_FORMAT);
                        try { sentDate = mdf.parse(dateStr); }
                        catch(ParseException e) { /* ignored */ }
                    }
                }
            }
        }

        if(sentDate==null) {
            Calendar msgDate = mapiMessage.getMessageDate();
            this.sentDate = msgDate.getTime();
        }
    }

    /**
     * Parses the attachments from the {@code mapiMessage}.
     * The parsing will continue, even if a chunk is not found.
     * 
     * @param mapiMessage The message to parse.
     * @throws ChunkNotFoundException If some data is not found in the {@code mapiMessage}.
     */
    protected void parseAttachments(MAPIMessage mapiMessage) throws ChunkNotFoundException {
        AttachmentChunks[] attachmentChunks = mapiMessage.getAttachmentFiles();
        for(AttachmentChunks attachmentChunk : attachmentChunks) {
            StringChunk longFileName = attachmentChunk.getAttachLongFileName();
            StringChunk fileName = attachmentChunk.getAttachFileName();
            ByteChunk data = attachmentChunk.getAttachData();
            StringChunk mimeType = attachmentChunk.getAttachMimeTag();

            String name = longFileName!=null ? longFileName.getValue() :
                          fileName!=null ?     fileName.getValue() :
                                               attachmentChunk.getPOIFSName();
            InputStream dataIS = data!=null ? new ByteArrayInputStream(data.getValue()) : null;
            String mimeTypeVal = mimeType!=null ? mimeType.getValue() : null;
            addAttachment(name, mimeTypeVal, dataIS);
        }
    }
    
    private boolean silent(SilentCallFailure call) {
        try { call.invoke(); }
        catch(ChunkNotFoundException ignored) { return false; }
        return true;
    }

    @FunctionalInterface
    private static interface SilentCallFailure {
        void invoke() throws ChunkNotFoundException;
    }
}
