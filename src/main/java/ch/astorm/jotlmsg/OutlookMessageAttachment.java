
package ch.astorm.jotlmsg;

import java.io.InputStream;

/**
 * Represents a message attachment.
 * 
 * @author Cedric Tabin
 */
public class OutlookMessageAttachment {
    private final String name;
    private InputStream inputStream;
    
    /**
     * Creates a new {@code OutlookMessageAttachment} with the specified parameters.
     * 
     * @param name The attachment's name.
     * @param input The input data or null.
     */
    public OutlookMessageAttachment(String name, InputStream input) {
        if(name==null || name.trim().isEmpty()) { throw new IllegalArgumentException("name is not defined"); }
        
        this.name = name;
        this.inputStream = input;
    }
    
    /**
     * Returns the name of the attachment, as seen by the user.
     * This value cannot be null nor empty.
     * 
     * @return The name of the attachment.
     */
    public final String getName() { 
        return name; 
    }
    
    /**
     * Defines the {@code InputStream} to use to access this attachment data.
     * This value may be null.
     */
    public InputStream getInputStream() { return inputStream; }
    public void setInputStream(InputStream is) { this.inputStream = is; }
}
