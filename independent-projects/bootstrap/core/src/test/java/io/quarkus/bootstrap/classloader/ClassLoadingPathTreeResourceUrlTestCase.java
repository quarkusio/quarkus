package io.quarkus.bootstrap.classloader;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.util.IoUtils;

public class ClassLoadingPathTreeResourceUrlTestCase {

    /**
     * URLClassLoader will return URL's that end with a / if the call to getResource ends with a /
     */
    @ParameterizedTest
    @MethodSource("paths")
    public void testUrlReturnedFromClassLoaderDirectory(String testPath) throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .add(new StringAsset("a"), "a.txt")
                .add(new StringAsset("b"), "b/b.txt");
        Path path = Files.createTempDirectory(testPath);
        try {
            jar.as(ExplodedExporter.class).exportExploded(path.toFile(), "tmp");

            ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                    .addNormalPriorityElement(ClassPathElement.fromPath(path.resolve("tmp"), true))
                    .build();
            URL res = cl.getResource("a.txt");
            Assertions.assertNotNull(res);
            Assertions.assertNull(res.getQuery());
            res = cl.getResource("a.txt/");
            Assertions.assertNull(res);

            res = cl.getResource("b");
            Assertions.assertNotNull(res);
            Assertions.assertFalse(res.toExternalForm().endsWith("/"));

            res = cl.getResource("b/");
            Assertions.assertNotNull(res);
            Assertions.assertTrue(res.toExternalForm().endsWith("/"));

            URL rootRes = cl.getResource("");
            Assertions.assertNotNull(rootRes);
            Assertions.assertTrue(rootRes.toExternalForm().startsWith("file:"));

            Enumeration<URL> rootResources = cl.getResources("");
            Assertions.assertTrue(rootResources.hasMoreElements());
            boolean found = false;
            while (rootResources.hasMoreElements()) {
                if (rootResources.nextElement().toExternalForm().equals(rootRes.toExternalForm())) {
                    found = true;
                }
            }
            Assertions.assertTrue(found);

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
    @ParameterizedTest
    @MethodSource("paths")
    public void testResourceAsStreamForDirectory(String testPath) throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .add(new StringAsset("a"), "a.txt")
                .add(new StringAsset("b"), "b/b.txt");
        final Path tmpDir = Files.createTempDirectory(testPath);
        try {
            jar.as(ExplodedExporter.class).exportExploded(tmpDir.toFile(), "tmpcltest");
            final ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                    .addNormalPriorityElement(ClassPathElement.fromPath(tmpDir.resolve("tmpcltest"), true))
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
    @ParameterizedTest
    @MethodSource("paths")
    public void testUrlReturnedFromClassLoaderJarFile(String testPath) throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .add(new StringAsset("a"), "a.txt")
                .add(new StringAsset("b"), "b/b.txt");
        Path path = Files.createTempFile(testPath, "quarkus-test.jar");
        try {
            jar.as(ZipExporter.class).exportTo(path.toFile(), true);

            ClassLoader cl = QuarkusClassLoader.builder("test", getClass().getClassLoader(), false)
                    .addNormalPriorityElement(ClassPathElement.fromPath(path, true))
                    .build();
            URL res = cl.getResource("a.txt");
            Assertions.assertNotNull(res);
            Assertions.assertNull(res.getQuery());
            res = cl.getResource("a.txt/");
            Assertions.assertNull(res);

            res = cl.getResource("b");
            Assertions.assertNotNull(res);
            Assertions.assertFalse(res.toExternalForm().endsWith("/"));

            res = cl.getResource("b/");
            Assertions.assertNotNull(res);
            Assertions.assertTrue(res.toExternalForm().endsWith("/"));

            Enumeration<URL> rootResources = cl.getResources("");
            while (rootResources.hasMoreElements()) {
                URL url = rootResources.nextElement();
                Assertions.assertFalse(url.toExternalForm().contains("quarkus-test.jar"),
                        "JAR root URL should not be returned by getResources(\"\"): " + url);
            }

        } finally {
            IoUtils.recursiveDelete(path);
        }
    }

    @Test
    public void testJarDoesNotReturnRootForEmptyResource() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .add(new StringAsset("a"), "a.txt");
        Path path = Files.createTempFile("test", "quarkus-test.jar");
        try {
            jar.as(ZipExporter.class).exportTo(path.toFile(), true);

            ClassLoader emptyParent = new ClassLoader(null) {
            };
            ClassLoader cl = QuarkusClassLoader.builder("test", emptyParent, false)
                    .addNormalPriorityElement(ClassPathElement.fromPath(path, true))
                    .build();
            Assertions.assertNull(cl.getResource(""));
            Assertions.assertFalse(cl.getResources("").hasMoreElements());
        } finally {
            IoUtils.recursiveDelete(path);
        }
    }

    public static Iterable<Object[]> paths() {
        if (OS.WINDOWS.isCurrentOs()) {
            return List.<Object[]> of(new Object[] { "test" });
        }
        return List.<Object[]> of(new Object[] { "test" }, new Object[] { "dir?with?question?mark" });
    }
}
