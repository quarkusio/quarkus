package io.quarkus.bootstrap.runner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JarResource}
 */
public class JarResourceTest {

    /**
     * Tests that the URL(s) returned from {@link JarResource#getResourceURL(String)} are properly encoded and can be used
     * to open connection to the URL to read data
     */
    @Test
    public void testResourceURLEncoding() throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        final String[] files = new String[] { "a.txt", "a b.txt", ",;~!@#$%^&().txt" };
        for (final String file : files) {
            jar.add(new StringAsset("hello"), file);
        }
        final Path testDir = Files.createTempDirectory("test");
        // create a child dir with special characters
        final Path specialCharDir = Files.createDirectory(Paths.get(testDir.toString(), ",;~!@#$%^&()"));
        final Path jarFilePath = Files.createTempFile(specialCharDir, "test", "quarkus-test.jar");
        // create a jar file under the directory which has the special characters
        jar.as(ZipExporter.class).exportTo(jarFilePath.toFile(), true);
        final JarResource jarResource = new JarResource(null, jarFilePath);
        for (final String resource : files) {
            final URL url = jarResource.getResourceURL(resource);
            Assertions.assertNotNull(url, resource + " is missing in jar");
            // check that opening the resource URL works and data can be read
            final URLConnection conn = url.openConnection();
            try (final InputStream is = conn.getInputStream()) {
                drainFully(is);
            }
        }
    }

    private static void drainFully(final InputStream inputStream) throws IOException {
        int read = -1;
        final byte[] data = new byte[1024];
        while ((read = inputStream.read(data)) != -1) {
        }
    }
}
