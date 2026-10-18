package io.quarkus.bootstrap.classloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.util.IoUtils;

public class MultiReleaseJarTestCase {

    Path jarPath;

    @BeforeEach
    void setUp(@TempDir Path tempDirectory) throws IOException {
        jarPath = tempDirectory.resolve("test.jar");
        try (OutputStream out = Files.newOutputStream(jarPath);
                JarOutputStream jos = new JarOutputStream(out)) {
            JarEntry entry = new JarEntry(JarFile.MANIFEST_NAME);
            jos.putNextEntry(entry);
            jos.write("Multi-Release: true\n".getBytes());
            jos.closeEntry();

            entry = new JarEntry("foo.txt");
            jos.putNextEntry(entry);
            jos.write("Original".getBytes());
            jos.closeEntry();

            entry = new JarEntry("META-INF/versions/9/foo.txt");
            jos.putNextEntry(entry);
            jos.write("MultiRelease".getBytes());
            jos.closeEntry();
        }
    }

    @Test
    public void shouldLoadMultiReleaseJarOnJDK9Plus() throws IOException {
        try (QuarkusClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                .addNormalPriorityElement(ClassPathElement.fromPath(jarPath, true))
                .build()) {
            URL resource = cl.getResource("foo.txt");
            assertNotNull(resource, "foo.txt was not found in generated JAR");
            assertTrue(resource.toString().contains("META-INF/versions/9"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = cl.getResourceAsStream("foo.txt")) {
                IoUtils.copy(baos, is);
            }
            assertEquals("MultiRelease", baos.toString());
        }
    }

}
