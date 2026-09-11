package io.quarkus.gradle.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

final class TestKitPluginClasspath {

    private TestKitPluginClasspath() {
    }

    static List<File> implementationClasspath() {
        Properties metadata = new Properties();
        try (InputStream stream = TestKitPluginClasspath.class.getClassLoader()
                .getResourceAsStream("plugin-under-test-metadata.properties")) {
            if (stream == null) {
                throw new IllegalStateException("Missing plugin-under-test-metadata.properties");
            }
            metadata.load(stream);
            String classpath = metadata.getProperty("implementation-classpath");
            if (classpath == null || classpath.isBlank()) {
                throw new IllegalStateException("Missing implementation-classpath in plugin-under-test metadata");
            }
            return Arrays.stream(classpath.split(Pattern.quote(File.pathSeparator)))
                    .map(File::new)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read plugin-under-test metadata", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create TestKit plugin classpath", e);
        }
    }

    static List<File> withTestClasses() {
        List<File> files = new ArrayList<>(implementationClasspath());
        try {
            files.add(Path.of(TestKitPluginClasspath.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toFile());
            return files;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create TestKit plugin classpath", e);
        }
    }
}
