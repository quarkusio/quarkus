package io.quarkus.modular.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JarEntryOutputStream}.
 */
class JarEntryOutputStreamTest {

    @Test
    void rejectsNameEndingWithSlash() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            assertThatThrownBy(() -> new JarEntryOutputStream(jos, "foo/"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Entry file name must not end with /");
        }
    }

    @Test
    void writeAndCloseSmallUncompressed() throws Exception {
        byte[] data = "Hello, world!".getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            try (JarEntryOutputStream os = new JarEntryOutputStream(jos, "test.txt", false, data.length)) {
                os.write(data);
            }
        }
        // read back and verify
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry = jis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("test.txt");
            assertThat(entry.getMethod()).isEqualTo(ZipEntry.STORED);
            byte[] read = jis.readAllBytes();
            assertThat(read).isEqualTo(data);
        }
    }

    @Test
    void writeAndCloseLargeUncompressed() throws Exception {
        // >8KB to exercise the temp file path
        byte[] data = new byte[16384];
        Arrays.fill(data, (byte) 'x');
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            try (JarEntryOutputStream os = new JarEntryOutputStream(jos, "large.bin", false, data.length)) {
                os.write(data);
            }
        }
        // read back and verify
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry = jis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("large.bin");
            assertThat(entry.getMethod()).isEqualTo(ZipEntry.STORED);
            byte[] read = jis.readAllBytes();
            assertThat(read).isEqualTo(data);
        }
    }

    @Test
    void writeAndCloseCompressed() throws Exception {
        byte[] data = "Compressed content for testing".getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            try (JarEntryOutputStream os = new JarEntryOutputStream(jos, "compressed.txt", true, data.length)) {
                os.write(data);
            }
        }
        // read back and verify
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry = jis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("compressed.txt");
            assertThat(entry.getMethod()).isEqualTo(ZipEntry.DEFLATED);
            byte[] read = jis.readAllBytes();
            assertThat(read).isEqualTo(data);
        }
    }

    @Test
    void closeIsIdempotent() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            JarEntryOutputStream os = new JarEntryOutputStream(jos, "test.txt");
            os.write("data".getBytes());
            os.close();
            os.close(); // second close should not throw
        }
    }

    @Test
    void writeAfterCloseThrows() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            JarEntryOutputStream os = new JarEntryOutputStream(jos, "test.txt");
            os.close();
            assertThatThrownBy(() -> os.write(42))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Entry closed");
        }
    }

    @Test
    void crcIsCorrectForStoredEntry() throws Exception {
        byte[] data = "Known data for CRC verification".getBytes();
        CRC32 expected = new CRC32();
        expected.update(data);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            try (JarEntryOutputStream os = new JarEntryOutputStream(jos, "crc.txt", false, data.length)) {
                os.write(data);
            }
        }
        // read back and verify CRC
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry = jis.getNextEntry();
            jis.readAllBytes(); // consume the entry so CRC is computed
            assertThat(entry.getCrc()).isEqualTo(expected.getValue());
        }
    }

    @Test
    void writeMethodVariantsAllWork() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            try (JarEntryOutputStream os = new JarEntryOutputStream(jos, "multi.txt", false, 100)) {
                os.write('A');
                os.write("BC".getBytes());
                os.write("xDEx".getBytes(), 1, 2);
            }
        }
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry = jis.getNextEntry();
            assertThat(entry).isNotNull();
            byte[] read = jis.readAllBytes();
            assertThat(new String(read)).isEqualTo("ABCDE");
        }
    }
}
