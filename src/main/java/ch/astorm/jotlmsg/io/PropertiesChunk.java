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

package ch.astorm.jotlmsg.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.poi.hsmf.datatypes.ChunkBasedPropertyValue;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.hsmf.datatypes.Types.MAPIType;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.util.LittleEndian;

/**
 * Handles the writing of {@link MAPIProperty} instances.
 * This file is based on {@link org.apache.poi.hsmf.datatypes.PropertiesChunk}.
 * <p>This class provides a basic implementation to write fixed-length and variable-length
 * properties to a {@link POIFSFileSystem}, but it doesn't handle the other data
 * such as list properties (since point 2.4.2.2) or GUID/Entry/String streams (point 2.2.3).
 * Those would be needed to make a more advanced integration with Microsoft Outlook and take
 * advantage of other features, like appointments, calendars, ...</p>
 * 
 * @author Cedric Tabin
 */
public class PropertiesChunk {
    
    /**
     * Standard prefix of nodes. Defined by the MS specification.
     */
    public static final String PREFIX = "__substg1.0_";
    
    /**
     * Flag to set for readable properties. Defined by the MS specification.
     */
    public static final int FLAG_READABLE = 2;

    /**
     * Flag to set for writable properties. Defined by the MS specification.
     */
    public static final int FLAG_WRITEABLE = 4;
    
    private final Map<MAPIProperty, PropertyValue> properties = new HashMap<>(64);

    /**
     * Defines a property. Multi-valued properties are not yet supported.
     */
    public void setProperty(PropertyValue value) { properties.put(value.getProperty(), value); }
    public PropertyValue getProperty(MAPIProperty property) { return properties.get(property); }
    
    /**
     * Writes this chunk in the specified {@code DirectoryEntry}.
     * 
     * @param directory The directory.
     * @throws IOException If an I/O error occurs.
     */
    public void writeTo(DirectoryEntry directory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<PropertyValue> values = writeHeaderData(baos);
        baos.close();
        
        //write the header data with the properties declaration
        directory.createDocument(org.apache.poi.hsmf.datatypes.PropertiesChunk.NAME, new ByteArrayInputStream(baos.toByteArray()));
        
        //write the property values
        writeNodeData(directory, values);
    }
    
    /**
     * Write the nodes for variable-length data.
     * Those properties are returned by {@link #writeHeaderData(java.io.OutputStream)}.
     * 
     * @param directory The directory.
     * @param values The values.
     * @throws IOException If an I/O error occurs.
     */
    protected void writeNodeData(DirectoryEntry directory, List<PropertyValue> values) throws IOException {
        for(PropertyValue value : values) {
            byte[] bytes = value.getRawValue();
            String nodeName = PREFIX+getFileName(value);
            directory.createDocument(nodeName, new ByteArrayInputStream(bytes));
        }
    }
    
    /**
     * Writes the header of the properties.
     * 
     * @param out The {@code OutputStream}.
     * @return The variable-length properties that need to be written in another node.
     * @throws IOException If an I/O error occurs.
     */
    protected List<PropertyValue> writeHeaderData(OutputStream out) throws IOException {
        List<PropertyValue> variableLengthProperties = new ArrayList<>(properties.size());
        for(Entry<MAPIProperty, PropertyValue> entry : properties.entrySet()) {
            MAPIProperty property = entry.getKey();
            PropertyValue value = entry.getValue();
            if(value==null) { continue; }
            if(value instanceof ChunkBasedPropertyValue) { throw new IOException("ChunkBasedPropertyValue not supported yet"); }
            
            //generic header
            //page 23, point 2.4.2
            int tag = Integer.parseInt(getFileName(value), 16); //tag is the property id and its type
            LittleEndian.putUInt(tag, out);
            LittleEndian.putUInt(value.getFlags(), out); //readable + writable

            MAPIType type = getTypeMapping(value.getActualType());
            if(type.isFixedLength()) { writeFixedLengthValueHeader(out, property, type, value); } //page 11, point 2.1.2
            else { //page 12, point 2.1.3
                writeVariableLengthValueHeader(out, property, type, value); 
                variableLengthProperties.add(value);
            } 
        }
        return variableLengthProperties;
    }
    
    private void writeFixedLengthValueHeader(OutputStream out, MAPIProperty property, MAPIType type, PropertyValue value) throws IOException {
        //fixed type header
        //page 24, point 2.4.2.1.1
        byte[] bytes = value.getRawValue(); //always return the bytes array
        int length = bytes!=null ? bytes.length : 0;
        if(bytes!=null) { 
            //because little endian
            out.write(bytes);
        }
        out.write(new byte[8-length]);
    }
    
    private void writeVariableLengthValueHeader(OutputStream out, MAPIProperty property, MAPIType type, PropertyValue value) throws IOException {
        //variable length header
        //page 24, point 2.4.2.2
        byte[] bytes = value.getRawValue(); //always return the bytes array
        int length = bytes!=null ? bytes.length : 0;

        //alter the length, as specified in page 25
        if(type==Types.UNICODE_STRING) { length += 2; }
        else if(type==Types.ASCII_STRING) { length += 1; }

        LittleEndian.putUInt(length, out);
        
        //specified in page 25
        LittleEndian.putUInt(0, out);
    }
    
    private String getFileName(PropertyValue propertyValue) {
        String str = Integer.toHexString(propertyValue.getProperty().id).toUpperCase(Locale.ROOT);
        while(str.length() < 4) {
            str = "0" + str;
        }
        
        MAPIType type = getTypeMapping(propertyValue.getActualType());
        return str + type.asFileEnding();
    }
    
    private MAPIType getTypeMapping(MAPIType type) {
        return type==Types.ASCII_STRING ? Types.UNICODE_STRING : type;
    }
}
