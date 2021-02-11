package io.quarkus.runner.classloading;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.classloading.ClassPathResource;
import io.quarkus.bootstrap.classloading.MemoryClassPathElement;

public class MemoryClassPathElementTestCase {

    static Map<String, byte[]> data;

    @BeforeAll
    public static void before() throws Exception {
        data = new HashMap<>();
        data.put("a.txt", "A file".getBytes(StandardCharsets.UTF_8));
        data.put("b.txt", "another file".getBytes(StandardCharsets.UTF_8));
        data.put("foo/sub.txt", "subdir file".getBytes(StandardCharsets.UTF_8));
    }

    @AfterAll
    public static void after() throws Exception {
        data = null;
    }

    @Test
    public void testGetAllResources() {
        MemoryClassPathElement f = new MemoryClassPathElement(data);
        Set<String> res = f.getProvidedResources();
        Assertions.assertEquals(3, res.size());
        Assertions.assertEquals(new HashSet<>(Arrays.asList("a.txt", "b.txt", "foo/sub.txt")), res);
    }

    @Test
    public void testGetResource() {
        MemoryClassPathElement f = new MemoryClassPathElement(data);
        ClassPathResource res = f.getResource("foo/sub.txt");
        Assertions.assertNotNull(res);
        Assertions.assertEquals("subdir file", new String(res.getData(), StandardCharsets.UTF_8));
    }
}
