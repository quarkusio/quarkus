package io.quarkus.bootstrap.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Tests {@link ZipUtils}
 */
public class ZipUtilsTest {

    /**
     * Test that when an operation on a corrupt zip file fails, the resulting IOException
     * has the path details of the file/uri of the source file
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/4126"/>
     */
    @Test
    public void testCorruptZipException() throws Exception {
        final Path tmpFile = Files.createTempFile(null, ".jar");
        try {
            final URI uri = new URI("jar", tmpFile.toUri().toString(), null);
            try {
                ZipUtils.newFileSystem(uri, Collections.emptyMap());
                Assertions.fail("New filesystem creation was expected to fail for a non-zip file");
            } catch (IOException ioe) {
                // verify the exception message content
                if (!ioe.getMessage().contains(uri.toString())) {
                    throw ioe;
                }
            }

            try {
                ZipUtils.newFileSystem(tmpFile);
                Assertions.fail("New filesystem creation was expected to fail for a non-zip file");
            } catch (IOException ioe) {
                // verify the exception message content
                if (!ioe.getMessage().contains(tmpFile.toString())) {
                    throw ioe;
                }
            }
        } finally {
            Files.delete(tmpFile);
        }
    }
}
