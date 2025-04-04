package io.quarkus.test.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Tests for PathTestHelper.
 */
public class PathTestHelperTest {
    @Test
    public void getTestClassesLocationForSimpleCase() {
        Path path = PathTestHelper.getTestClassesLocation(this.getClass());
        assertNotNull(path);
        assertTrue(path.toString().contains("target" + File.separator + "test-classes"), path.toString());
    }

    @Test
    public void getTestClassesLocationForJar() {

        // Choose a class which is likely to be on the classpath, but not in the source tree; this isn't a test class because we don't have many of them in jars, but that should still work
        Path path = PathTestHelper.getTestClassesLocation(org.jboss.logging.Logger.class);
        assertNotNull(path);
        assertTrue(path.toString().contains(".jar"), path.toString());
    }
}
