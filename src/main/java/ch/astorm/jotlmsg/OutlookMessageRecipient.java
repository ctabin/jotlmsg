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
