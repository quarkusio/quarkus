package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.test.junit.QuarkusTest;

/**
 * The purpose of this test is to ensure that using the projection domain of a class loaded
 * from the QuarkusClassLoader yields the appropriate result
 */
@QuarkusTest
public class QuarkusClassloaderProtectionDomainTest {

    @Test
    public void testClassFromJar() throws IOException {
        Class<?> testClass = BeanManager.class;
        ClassLoader classLoader = testClass.getClassLoader();
        assertTrue(classLoader instanceof QuarkusClassLoader);

        URL location = testClass.getProtectionDomain().getCodeSource().getLocation();
        assertNotNull(location);

        try (InputStream inputStream = location.openStream()) {
            try (JarInputStream jarInputStream = new JarInputStream(inputStream)) {
                Manifest manifest = jarInputStream.getManifest();
                assertNotNull(manifest);
                assertEquals("jakarta.enterprise.cdi-api", manifest.getMainAttributes().getValue("Bundle-SymbolicName"));
            }
        }
    }

}
