package ch.astorm.jotlmsg.io;

import java.nio.ByteBuffer;

/**
 * Represents an [MS-OXCDATA] 2.3.2 FlatEntry Structure in Java.
 * 
 * @author Guido Stein
 */
public class FlatEntryStructure {
    private long size; // Number of bytes of following EntryID field.
    private byte[] entryID;

    /**
     * Returns the number of bytes of following EntryID field.
     * 
     * @return Number of bytes.
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the EntryID bytes.
     * 
     * @return Byte array representation of EntryID.
     */
    public byte[] getEntryID() {
        return entryID;
    }

    /**
     * Sets the EntryID bytes.
     * 
     * @param entryID Byte array data to set.
     */
    public void setEntryID(byte[] entryID) {
        this.entryID = entryID;
        this.size = entryID.length;
    }

    /**
     * Default constructor
     */
    public FlatEntryStructure() {
    };

    /**
     * Constructor for byte parsing.
     * 
     * @param bf {@link java.nio.ByteBuffer} source for parsing. 
     */
    public FlatEntryStructure(ByteBuffer bf) {
        // Size is stored in 4 bytes.
        size = bf.getInt();

        // Copy entry bytes from ByteBuffer.
        entryID = new byte[Long.valueOf(size).intValue()];
        bf.get(entryID, 0, Long.valueOf(size).intValue());
    }
}
