package ch.astorm.jotlmsg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.poi.util.StringUtil;

/**
 * Represents an [MS-OXCDATA] 2.2.5.1 One-Off EntryID Structure in Java.
 * 
 * @author Guido Stein
 *
 */
public class OneOffEntryIDStructure extends FlatEntryStructure {
    // Flags indicate a long-term EntryID.
    private static final byte[] FLAGS = { 0, 0, 0, 0 };

    // Provider UUID is always fix.
    private static final byte[] UUID = { (byte) 0x81, (byte) 0x2B, (byte) 0x1F, (byte) 0xA4, (byte) 0xBE, (byte) 0xA3,
            (byte) 0x10, (byte) 0x19, (byte) 0x9D, (byte) 0x6E, (byte) 0x00, (byte) 0xDD, (byte) 0x01, (byte) 0x0F,
            (byte) 0x54, (byte) 0x02 };

    // Version is 0x0000.
    private static final byte[] VERSION = { 0, 0 };

    // Pad MAE Format M U R L Pad
    private static final byte[] PAD_MAE_FORMAT_M_U_R_L_PAD = { 1, -112, };

    // AddressType is SMTP.
    private static final byte[] ADDRESSTYPE = { 83, 0, 77, 0, 84, 0, 80, 0, 0, 0 };

    private String displayName = null;
    private String emailAddress = null;

    /**
     * Constructor for byte parsing.
     * 
     * @param bf
     */
    public OneOffEntryIDStructure(ByteBuffer bf) {
        super(bf);

        // Allocate a new ByteBuffer.
        ByteBuffer bf2 = ByteBuffer.wrap(getEntryID()).order(ByteOrder.LITTLE_ENDIAN);

        // Set position to begin of DisplayName.
        bf2.position(OneOffEntryIDStructure.FLAGS.length + OneOffEntryIDStructure.UUID.length
                + OneOffEntryIDStructure.VERSION.length + OneOffEntryIDStructure.PAD_MAE_FORMAT_M_U_R_L_PAD.length);
        int remaining = bf2.remaining();
        byte[] unicodeLEStrings = new byte[remaining];
        bf2.get(unicodeLEStrings, 0, remaining);

        displayName = StringUtil.getFromUnicodeLE0Terminated(unicodeLEStrings, 0, remaining / 2);
        int offset = displayName.length() * 2 + 2;
        String addressType = StringUtil.getFromUnicodeLE0Terminated(unicodeLEStrings, offset, (remaining - offset) / 2);
        offset += addressType.length() * 2 + 2;
        emailAddress = StringUtil.getFromUnicodeLE0Terminated(unicodeLEStrings, offset, (remaining - offset) / 2);
    }

    /**
     * Constructor for byte conversion.
     * 
     * @param emailAddress
     */
    public OneOffEntryIDStructure(String emailAddress) {
        this(emailAddress, emailAddress);
    }

    /**
     * Constructor for byte conversion.
     * 
     * @param displayName
     * @param emailAddress
     */
    public OneOffEntryIDStructure(String displayName, String emailAddress) {
        super();

        // Calculate total size of ByteBuffer
        long totalSize = OneOffEntryIDStructure.FLAGS.length + OneOffEntryIDStructure.UUID.length
                + OneOffEntryIDStructure.VERSION.length + OneOffEntryIDStructure.PAD_MAE_FORMAT_M_U_R_L_PAD.length;

        byte[] displayNameBytes = StringUtil.getToUnicodeLE(displayName);
        totalSize += displayNameBytes.length + 2; // 2-byte terminating null character

        totalSize += OneOffEntryIDStructure.ADDRESSTYPE.length;

        byte[] emailAddressBytes = StringUtil.getToUnicodeLE(emailAddress);
        totalSize += emailAddressBytes.length + 2; // 2-byte terminating null character

        // Allocate the complete ByteBuffer.
        ByteBuffer bf = ByteBuffer.allocate((int) totalSize).order(ByteOrder.LITTLE_ENDIAN);

        // Fill ByteBuffer.
        bf.put(OneOffEntryIDStructure.FLAGS).put(OneOffEntryIDStructure.UUID).put(OneOffEntryIDStructure.VERSION)
                .put(OneOffEntryIDStructure.PAD_MAE_FORMAT_M_U_R_L_PAD);
        bf.put(displayNameBytes).put((byte) 0).put((byte) 0);
        bf.put(OneOffEntryIDStructure.ADDRESSTYPE);
        bf.put(emailAddressBytes).put((byte) 0).put((byte) 0);

        // Fill FlatEntryStructure's entryID.
        setEntryID(bf.array());
    }

    /**
     * Returns the displayName.
     * 
     * @return
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the emailAddress.
     * 
     * @return
     */
    public String getEmailAddress() {
        return emailAddress;
    }
}
