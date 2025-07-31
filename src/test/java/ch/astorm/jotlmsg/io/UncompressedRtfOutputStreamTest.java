package ch.astorm.jotlmsg.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.apache.poi.hmef.CompressedRTF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class UncompressedRtfOutputStreamTest {

    @Test
    public void uncompressedStream_shouldBeDecompressableByPOI() throws IOException {
        // given
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String testString = "Good evening!";

        // when
        try (UncompressedRtfOutputStream sut = new UncompressedRtfOutputStream(baos)) {
            try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(sut)) {
                outputStreamWriter.append(testString);
                outputStreamWriter.flush();
            }
        }

        // then
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ByteArrayOutputStream resultBytes = new ByteArrayOutputStream();
        new CompressedRTF().decompress(bais, resultBytes);
        assertEquals(testString, resultBytes.toString());
    }
}
