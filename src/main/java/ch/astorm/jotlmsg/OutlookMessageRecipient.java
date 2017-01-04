
package ch.astorm.jotlmsg;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Represents a recipient.
 * 
 * @author Cedric Tabin
 */
public class OutlookMessageRecipient {

    /**
     * Represents the type of recipient.
     */
    public static enum Type {
        /**
         * The main recipient.
         */
        TO(RecipientType.TO),
        
        /**
         * The recipient that will receive a copy.
         */
        CC(RecipientType.CC),
        
        /**
         * The recipient that will receive a carbon copy.
         */
        BCC(RecipientType.BCC);
        
        private RecipientType rt;
        private Type(RecipientType rt) { this.rt = rt; }
        
        /**
         * Returns the corresponding {@code RecipientType} from the {@link Message}
         * API.
         * 
         * @return The recipient type.
         */
        RecipientType getRecipientType() { return rt; }
    }
    
    private final Type type;
    private String name;
    private String email;

    /**
     * Creates a new {@code OutlookMessageRecipient} with the given {@code type} and
     * {@code email} and a null name.
     * 
     * @param type The type.
     * @param email The email or null.
     */
    public OutlookMessageRecipient(Type type, String email) {
        this(type, email, null);
    }
    
    /**
     * Creates a new {@code OutlookMessageRecipient} with the specified parameters.
     * 
     * @param type The type.
     * @param email The email or null.
     * @param name The name or null.
     */
    public OutlookMessageRecipient(Type type, String email, String name) {
        if(type==null) { throw new IllegalArgumentException("type not defined"); }
        
        this.type = type;
        this.email = email;
        this.name = name;
    }
    
    /**
     * Returns the type of recipient.
     * 
     * @return The type of recipient.
     */
    public final Type getType() { return type; }

    /**
     * Defines the name of the recipient. 
     * This value may be null.
     */
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /**
     * Defines the email of the recipient. 
     * This value may be null.
     */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    /**
     * Returns a new {@code Address} from the email of this recipient. If the email
     * is not defined, this method returns null.
     * 
     * @return A new {@code InternetAddress}.
     * @throws AddressException If the email is not valid.
     */
    public Address getAddress() throws AddressException {
        if(email==null || email.isEmpty()) { return null; }
        return new InternetAddress(email);
    }
}
