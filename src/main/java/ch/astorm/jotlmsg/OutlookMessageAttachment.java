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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.util.IOUtils;

/**
 * Represents a message attachment.
 * 
 * @author Cedric Tabin
 */
public class OutlookMessageAttachment {
    private final String name;
    private String mimeType;
    private InputStreamCreator inputStreamCreator;
    
    /**
     * Represents an {@code InputStream} creator.
     */
    public static interface InputStreamCreator {
        
        /**
         * Creates a new {@code InputStream} for the specified {@code OutlookMessageAttachment}.
         * 
         * @param omt The attachment.
         * @return A new {@code InputStream}.
         * @throws IOException If an I/O error occurs.
         */
        InputStream newInputStream(OutlookMessageAttachment omt) throws IOException;
    }
    
    /**
     * Wraps the creation of an {@code InputStream} from a given source {@code InputStream}.
     * The content of the source {@code InputStream} will be stored in-memory.
     */
    public static class MemoryInputStreamCreator implements InputStreamCreator {
        private InputStream source;
        private byte[] content;
        
        /**
         * Creates a new {@code MemoryInputStreamCreator} with the specified {@code source}.
         * 
         * @param source The source.
         */
        public MemoryInputStreamCreator(InputStream source) {
            if(source==null) { throw new IllegalArgumentException("source is not defined"); }
            this.source = source;
        }
        
        /**
         * Reads a new {@code InputStream} with the content of the source.
         * The first time this method is called, the source {@code InputStream} will be fully
         * read and stored in-memory. Then, a new {@code ByteArrayInputStream} is
         * returned.
         * 
         * @param omt The attachment.
         * @return A new {@code ByteArrayInputStream}.
         * @throws IOException If an I/O error occurs.
         */
        @Override
        public InputStream newInputStream(OutlookMessageAttachment omt) throws IOException {
            if(content==null) { 
                content = IOUtils.toByteArray(source); 
                source = null;
            }
            return new ByteArrayInputStream(content);
        }
    }
    
    /**
     * Creates a new {@code OutlookMessageAttachment} with the specified parameters.
     * 
     * @param name The attachment's name.
     * @param mimeType The MIME type of the attachment.
     * @param creator The {@code InputStreamCreator} or null.
     */
    public OutlookMessageAttachment(String name, String mimeType, InputStreamCreator creator) {
        if(name==null || name.trim().isEmpty()) { throw new IllegalArgumentException("name is not defined"); }
        
        this.name = name;
        this.mimeType = mimeType;
        this.inputStreamCreator = creator;
    }
    
    /**
     * Creates a new {@code OutlookMessageAttachment} with the specified parameters.
     * A new {@code MemoryInputStreamCreator} will be created with the specified {@code input}
     * as source.
     * 
     * @param name The attachment's name.
     * @param mimeType The MIME type of the attachment.
     * @param input The input or null.
     * @see MemoryInputStreamCreator
     */
    public OutlookMessageAttachment(String name, String mimeType, InputStream input) {
        this(name, mimeType, input!=null ? new MemoryInputStreamCreator(input) : null);
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
     * Defines the MIME type of the attachment.
     */
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    /**
     * Defines the {@code InputStreamCreator} that handles the attachment content.
     * This value may be null.
     */
    public InputStreamCreator getInputStreamCreator() { return inputStreamCreator; }
    public void setInputStreamCreator(InputStreamCreator is) { this.inputStreamCreator = is; }
    
    /**
     * Returns a new {@code InputStream} to read the content of this attachment.
     * 
     * @return A new {@code InputStream}.
     * @throws IOException If an I/O error occurs.
     * @see InputStreamCreator#newInputStream(ch.astorm.jotlmsg.OutlookMessageAttachment)
     */
    public InputStream getNewInputStream() throws IOException {
        if(inputStreamCreator==null) { throw new IllegalStateException("missing input stream creator"); }
        return inputStreamCreator.newInputStream(this);
    }
}
