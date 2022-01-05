package io.quarkus.bootstrap.classloader;

import io.quarkus.bootstrap.classloading.DirectoryClassPathElement;
import io.quarkus.bootstrap.classloading.JarClassPathElement;
import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.util.IoUtils;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

//see https://github.com/quarkusio/quarkus/issues/10943
public class ClassLoadingResourceUrlTestCase {

    /**
     * URLClassLoader will return URL's that end with a / if the call to getResource ends with a /
     */
    @Test
    public void testUrlReturnedFromClassLoaderDirectory() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .add(new StringAsset("a"), "a.txt")
                .add(new StringAsset("b"), "b/b.txt");
        Path path = Files.createTempDirectory("test");
        try {
            jar.as(ExplodedExporter.class).exportExploded(path.toFile(), "tmp");

            ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                    .addElement(new DirectoryClassPathElement(path.resolve("tmp")))
                    .build();
            URL res = cl.getResource("a.txt");
            Assertions.assertNotNull(res);
            res = cl.getResource("a.txt/");
            Assertions.assertNull(res);

            res = cl.getResource("b");
            Assertions.assertNotNull(res);
            Assertions.assertFalse(res.toExternalForm().endsWith("/"));

            res = cl.getResource("b/");
            Assertions.assertNotNull(res);
            Assertions.assertTrue(res.toExternalForm().endsWith("/"));

        } finally {
            IoUtils.recursiveDelete(path);
        }
    }

    /**
     * Test that {@link QuarkusClassLoader#getResourceAsStream(String)} returns a stream for directory
     * resources
     *
     * @throws Exception
     * @see <a href="https://github.com/quarkusio/quarkus/issues/11707"/>
     */
    @Test
    public void testResourceAsStreamForDirectory() throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .add(new StringAsset("a"), "a.txt")
                .add(new StringAsset("b"), "b/b.txt");
        final Path tmpDir = Files.createTempDirectory("test");
        try {
            jar.as(ExplodedExporter.class).exportExploded(tmpDir.toFile(), "tmpcltest");
            final ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                    .addElement(new DirectoryClassPathElement(tmpDir.resolve("tmpcltest")))
                    .build();

            try (final InputStream is = cl.getResourceAsStream("b/")) {
                Assertions.assertNotNull(is, "InputStream is null for a directory resource");
            }
            try (final InputStream is = cl.getResourceAsStream("b")) {
                Assertions.assertNotNull(is, "InputStream is null for a directory resource");
            }
        } finally {
            IoUtils.recursiveDelete(tmpDir);
        }
    }

    /**
     * URLClassLoader will return URL's that end with a / if the call to getResource ends with a /
     */
    @Test
    public void testUrlReturnedFromClassLoaderJarFile() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .add(new StringAsset("a"), "a.txt")
                .add(new StringAsset("b"), "b/b.txt");
        Path path = Files.createTempFile("test", "quarkus-test.jar");
        try {
            jar.as(ZipExporter.class).exportTo(path.toFile(), true);

            ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                    .addElement(new JarClassPathElement(path))
                    .build();
            URL res = cl.getResource("a.txt");
            Assertions.assertNotNull(res);
            res = cl.getResource("a.txt/");
            Assertions.assertNull(res);

            res = cl.getResource("b");
            Assertions.assertNotNull(res);
            Assertions.assertFalse(res.toExternalForm().endsWith("/"));

            res = cl.getResource("b/");
            Assertions.assertNotNull(res);
            Assertions.assertTrue(res.toExternalForm().endsWith("/"));

        } finally {
            IoUtils.recursiveDelete(path);
        }
    }

    @Test
    public void testMemoryUrlConnections() throws Exception {
        long start = System.currentTimeMillis();
        Thread.sleep(2);

        ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                .addElement(
                        new MemoryClassPathElement(Collections.singletonMap("a.txt", "hello".getBytes(StandardCharsets.UTF_8))))
                .build();
        URL res = cl.getResource("a.txt");
        Assertions.assertNotNull(res);
        URLConnection urlConnection = res.openConnection();
        Assertions.assertEquals(5, urlConnection.getContentLength());
        Assertions.assertTrue(urlConnection.getLastModified() > start);
        Assertions.assertEquals("hello", new String(urlConnection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

    }
}
