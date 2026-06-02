package io.quarkus.modular.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JarDirEntry}.
 */
class JarDirEntryTest {

    @Test
    void rejectsNameWithoutTrailingSlash() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            assertThatThrownBy(() -> new JarDirEntry(jos, "foo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Directory name must end with /");
        }
    }

    @Test
    void acceptsNameWithTrailingSlash() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            try (JarDirEntry dir = new JarDirEntry(jos, "foo/")) {
                // should succeed without exception
            }
        }
        // verify the entry exists and is STORED with zero size
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry = jis.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("foo/");
            assertThat(entry.getMethod()).isEqualTo(ZipEntry.STORED);
            assertThat(entry.getSize()).isZero();
        }
    }

    @Test
    void closeIsIdempotent() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            JarDirEntry dir = new JarDirEntry(jos, "bar/");
            dir.close();
            dir.close(); // second close should not throw
        }
    }
}
