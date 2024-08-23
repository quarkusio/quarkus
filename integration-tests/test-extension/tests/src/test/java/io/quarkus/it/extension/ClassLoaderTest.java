package io.quarkus.it.extension;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClassLoaderTest {

    @Test
    void testClassLoaderResources() throws IOException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ArrayList<URL> resources = Collections.list(contextClassLoader.getResources("io/quarkus/it/extension"));
        Assertions.assertEquals(2, resources.size());
    }

    @Test
    void testClassLoaderSingleResource() throws IOException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL resource = contextClassLoader.getResource("io/quarkus/it/extension/my_resource.txt");
        Assertions.assertNotNull(resource);
    }
}
