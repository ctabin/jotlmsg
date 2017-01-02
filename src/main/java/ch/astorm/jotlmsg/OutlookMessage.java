
package ch.astorm.jotlmsg;

import ch.astorm.jotlmsg.OutlookMessageRecipient.Type;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an Outlook message.
 * 
 * @author Cedric Tabin
 */
public class OutlookMessage {
    private String subject;
    private String plainTextBody;
    
    private final Map<Type, List<OutlookMessageRecipient>> recipients = new EnumMap<>(Type.class);
    private final List<OutlookMessageAttachment> attachments = new ArrayList<>(8);

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
     * Returns all the recipients of the specified type. If there is none, then an
     * empty list will be returned.
     * 
     * @param type The recipients type.
     * @return An immutable list with all the recipients of the given type.
     */
    public List<OutlookMessageRecipient> getRecipients(Type type) { return Collections.unmodifiableList(recipients.getOrDefault(type, new ArrayList<>(0))); }
    
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
     * @param input The input data or null.
     * @return The created attachment.
     */
    public OutlookMessageAttachment addAttachment(String name, InputStream input) {
        OutlookMessageAttachment attachment = new OutlookMessageAttachment(name, input);
        attachments.add(attachment);
        return attachment;
    }
}
