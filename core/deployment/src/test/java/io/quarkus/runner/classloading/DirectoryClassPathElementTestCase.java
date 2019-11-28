package io.quarkus.runner.classloading;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.classloading.ClassPathResource;
import io.quarkus.bootstrap.classloading.DirectoryClassPathElement;
import io.quarkus.deployment.util.FileUtil;

public class DirectoryClassPathElementTestCase {

    static Path root;

    @BeforeAll
    public static void before() throws Exception {
        root = Files.createTempDirectory("quarkus-test");
        Files.write(root.resolve("a.txt"), "A file".getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("b.txt"), "another file".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("foo"));
        Files.write(root.resolve("foo/sub.txt"), "subdir file".getBytes(StandardCharsets.UTF_8));
    }

    @AfterAll
    public static void after() throws Exception {
        FileUtil.deleteDirectory(root);
    }

    @Test
    public void testGetAllResources() {
        DirectoryClassPathElement f = new DirectoryClassPathElement(root);
        Set<String> res = f.getProvidedResources();
        Assertions.assertEquals(4, res.size());
        Assertions.assertEquals(new HashSet<>(Arrays.asList("a.txt", "b.txt", "foo", "foo/sub.txt")), res);
    }

    @Test
    public void testGetResource() {
        DirectoryClassPathElement f = new DirectoryClassPathElement(root);
        ClassPathResource res = f.getResource("foo/sub.txt");
        Assertions.assertNotNull(res);
        Assertions.assertEquals("subdir file", new String(res.getData(), StandardCharsets.UTF_8));
    }
}
