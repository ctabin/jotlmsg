package ch.astorm.jotlmsg.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.hmef.CompressedRTF;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;

/**
 * Uncompressed RTF {@link OutputStream} for basic RTF support.
 */
public class UncompressedRtfOutputStream extends FilterOutputStream {

    private static final byte[] CRC_UNCOMPRESSED = new byte[] { 0x00, 0x00, 0x00, 0x00 };

    private boolean closed = false;
    private final Object closeLock = new Object();

    private final ByteArrayOutputStream dataBuffer;
    private int rawSize;

    public UncompressedRtfOutputStream(OutputStream delegate) {
        super(delegate);
        dataBuffer = new ByteArrayOutputStream();
        rawSize = 0;
    }

    @Override
    public void write(final int b) {
        dataBuffer.write(b);
        rawSize += 1;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        writeHeader();
        writeData();
        super.close();
    }

    void writeHeader() throws IOException {
        LittleEndian.putInt(dataBuffer.size() + 12, this.out);
        LittleEndian.putInt(rawSize, this.out);
        LittleEndian.putInt(CompressedRTF.UNCOMPRESSED_SIGNATURE_INT, out);
        this.out.write(CRC_UNCOMPRESSED);
    }

    void writeData() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dataBuffer.toByteArray());
        IOUtils.copy(byteArrayInputStream, this.out);
    }
}
