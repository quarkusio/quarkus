package io.quarkus.bootstrap.runner;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

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
        testInternal("test");
    }

    /**
     * As above but with a question mark in the path
     * <p>
     * Disabled on windows as such paths are not allowed
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testResourceURLEncodingWithQuestionMark() throws Exception {
        testInternal("test?dir");
    }

    /**
     * Tests that the URL(s) returned from {@link JarResource#getResourceURL(String)} are properly encoded and can be used
     * to open connection to the URL to read data
     */
    private void testInternal(String path) throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        final String[] files = new String[] { "a.txt", "a b.txt", ",;~!@#$%^&().txt" };
        for (final String file : files) {
            jar.add(new StringAsset("hello"), file);
        }
        final Path testDir = Files.createTempDirectory(path);
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

    /**
     * @see <a href="https://github.com/quarkusio/quarkus/issues/56173">#56173</a>
     */
    @Test
    public void testCodeSourceUrlExternalFormIsCached() throws Exception {
        Path dir = Files.createTempDirectory("codesource-cache");
        Path jarFile = dir.resolve("demo.jar");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.add(new StringAsset("dummy"), "dummy");
        jar.as(ZipExporter.class).exportTo(jarFile.toFile(), true);

        JarResource resource = new JarResource(null, jarFile);
        resource.init();
        URL codeSource = resource.getProtectionDomain().getCodeSource().getLocation();

        String first = codeSource.toExternalForm();
        String second = codeSource.toExternalForm();
        assertThat(first).isSameAs(second);
    }

    /**
     * @see <a href="https://github.com/quarkusio/quarkus/issues/56173">#56173</a>
     */
    @Test
    public void testRelativeUrlResolutionAgainstCodeSource() throws Exception {
        Path dir = Files.createTempDirectory("codesource");
        Path jarFile = dir.resolve("demo.jar");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.add(new StringAsset("dummy"), "dummy");
        jar.as(ZipExporter.class).exportTo(jarFile.toFile(), true);

        JarResource resource = new JarResource(null, jarFile);
        resource.init();
        URL codeSource = resource.getProtectionDomain().getCodeSource().getLocation();

        URL sibling = new URL(codeSource, "sibling.txt");

        assertThat(sibling.toString()).isNotEqualTo(codeSource.toString());
        assertThat(sibling.toString()).endsWith("sibling.txt");
        assertThat(sibling.toString()).doesNotContain("demo.jar");
    }

    /**
     * @see <a href="https://github.com/quarkusio/quarkus/issues/56173">#56173</a>
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testRelativeUrlResolutionWithSpecialCharacters() throws Exception {
        Path dir = Files.createTempDirectory("code source dir");
        Path jarFile = dir.resolve("demo.jar");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.add(new StringAsset("dummy"), "dummy");
        jar.as(ZipExporter.class).exportTo(jarFile.toFile(), true);

        String siblingContent = "sibling in special dir";
        Files.writeString(dir.resolve("sibling.txt"), siblingContent);

        JarResource resource = new JarResource(null, jarFile);
        resource.init();
        URL codeSource = resource.getProtectionDomain().getCodeSource().getLocation();

        assertThat(codeSource.toString()).contains("code%20source%20dir");

        URL sibling = new URL(codeSource, "sibling.txt");

        assertThat(sibling.toString()).endsWith("/sibling.txt");
        assertThat(sibling.toString()).contains("code%20source%20dir");

        try (InputStream in = sibling.openStream()) {
            assertThat(new String(in.readAllBytes())).isEqualTo(siblingContent);
        }
    }

    private static void drainFully(final InputStream inputStream) throws IOException {
        int read = -1;
        final byte[] data = new byte[1024];
        while ((read = inputStream.read(data)) != -1) {
        }
    }
}
