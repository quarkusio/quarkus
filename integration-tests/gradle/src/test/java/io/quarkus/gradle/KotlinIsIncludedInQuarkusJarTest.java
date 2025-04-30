package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

public class KotlinIsIncludedInQuarkusJarTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testFastJarFormatWorks() throws Exception {

        final File projectDir = getProjectDir("basic-kotlin-application-project");

        runGradleWrapper(projectDir, "clean", "build");

        final Path quarkusApp = projectDir.toPath().resolve("build").resolve("quarkus-app").resolve("app");
        assertThat(quarkusApp).exists();
        Path jar = quarkusApp.resolve("code-with-quarkus-unspecified.jar");
        assertThat(jar).exists();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            assertJarContainsEntry(jarFile, "basic-kotlin-application-project/src/main/kotlin/org/acme/MyMainClass.class");
            assertJarContainsEntry(jarFile, "org/acme/GreetingResource.class");
            assertJarContainsEntry(jarFile, "META-INF/code-with-quarkus.kotlin_module");
        }
    }

    private void assertJarContainsEntry(JarFile jarFile, String expectedEntry) {
        boolean entryFound = false;
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            System.out.println(entry.getName());
            if (entry.getName().equals(expectedEntry)) {
                entryFound = true;
                break;
            }
        }
        assertTrue(entryFound, "Expected entry " + expectedEntry + " not found in JAR file.");
    }
}
