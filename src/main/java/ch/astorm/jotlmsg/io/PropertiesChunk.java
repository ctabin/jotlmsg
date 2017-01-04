
package ch.astorm.jotlmsg.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.poi.hsmf.datatypes.ChunkBasedPropertyValue;
import org.apache.poi.hsmf.datatypes.MAPIProperty;
import org.apache.poi.hsmf.datatypes.PropertyValue;
import org.apache.poi.hsmf.datatypes.Types;
import org.apache.poi.hsmf.datatypes.Types.MAPIType;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.util.LittleEndian;

/**
 * Handles the writing of {@link MAPIProperty} instances.
 * This file is based on {@link org.apache.poi.hsmf.datatypes.PropertiesChunk}.
 * 
 * @author Cedric Tabin
 */
public class PropertiesChunk {
    
    //standard prefix, defined in the spec
    public static final String PREFIX = "__substg1.0_";
    
    //standard property flags, defined in the spec
    public static final int FLAG_READABLE = 2;
    public static final int FLAG_WRITEABLE = 4;
    
    private Map<MAPIProperty, PropertyValue> properties = new HashMap<MAPIProperty, PropertyValue>();
    private boolean containsStorageAttachment;
    
    /**
     * Defines if there is a storage attachment. This will change how the size of variable-length
     * properties are set.
     * See specification, page 25
     */
    public boolean containsStorageAttachment() { return containsStorageAttachment; }
    public void setConstainsStorageAttachment(boolean v) { containsStorageAttachment = v; }
    
    /**
     * Defines a property. Multi-valued properties are not (yet?) supported.
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
            byte[] bytes = (byte[])value.getValue();
            String nodeName = PREFIX+value.getProperty().asFileName();
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
        List<PropertyValue> variableLengthProperties = new ArrayList<PropertyValue>();
        for(Entry<MAPIProperty, PropertyValue> entry : properties.entrySet()) {
            MAPIProperty property = entry.getKey();
            PropertyValue value = entry.getValue();
            if(value==null) { continue; }
            if(value instanceof ChunkBasedPropertyValue) { throw new IOException("ChunkBasedPropertyValue not supported yet"); }
            
            //generic header
            //page 23, point 2.4.2
            int tag = Integer.parseInt(property.asFileName(), 16); //tag is the property id and its type
            LittleEndian.putUInt(tag, out);
            LittleEndian.putUInt(value.getFlags(), out); //readable + writable
            
            MAPIType type = getType(property);
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
        byte[] bytes = (byte[])value.getValue(); //always return the bytes array
        int length = bytes!=null ? bytes.length : 0;
        if(bytes!=null) { out.write(bytes); }
        out.write(new byte[8-length]);
    }
    
    private void writeVariableLengthValueHeader(OutputStream out, MAPIProperty property, MAPIType type, PropertyValue value) throws IOException {
        //variable length header
        //page 24, point 2.4.2.2
        if(!containsStorageAttachment) {
            byte[] bytes = (byte[])value.getValue(); //always return the bytes array
            int length = bytes!=null ? bytes.length : 0;

            //alter the length, as specified in page 25
            if(type==Types.UNICODE_STRING) { length += 2; }
            else if(type==Types.ASCII_STRING) { length += 1; }

            LittleEndian.putUInt(length, out);
        } else {
            LittleEndian.putUInt(0xFFFFFFFF, out);
        }
        
        //specified in page 25
        if(containsStorageAttachment) { LittleEndian.putInt(0x04, out); }
        //else if(containsEmbeddedMessage) { LittleEndian.putInt(0x01, out); } //not supported
        else { LittleEndian.putUInt(0, out); }
    }
    
    /*
    Unfortunately, there is no way to access the type in MAPIProperty yet. Hence we
    get it through reflection.
    */
    private MAPIType getType(MAPIProperty property) {
        try {
            Field usualType = property.getClass().getDeclaredField("usualType");
            return (MAPIType)usualType.get(property);
        } catch(Exception e) {
            throw new RuntimeException("unable to retrieve type of property "+property, e);
        }
    }
}