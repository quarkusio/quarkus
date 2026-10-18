package io.quarkus.codestarts.tooling.wrapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class GradleWrapperVersionTest {

    /**
     * Assert that the bundled Gradle wrapper version matches ${gradle-wrapper.version} POM property value.
     */
    @Test
    public void testBundledGradleWrapperVersion() {
        final Path wrapperPropsPath = Path
                .of("target/classes/codestarts/quarkus/tooling/gradle-wrapper/base/gradle/wrapper/gradle-wrapper.properties");
        assertThat(wrapperPropsPath).exists();

        final Properties wrapperProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(wrapperPropsPath)) {
            wrapperProps.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(wrapperProps).hasEntrySatisfying("distributionUrl",
                value -> assertThat(value.toString()).contains("gradle-" + System.getProperty("gradle-wrapper.version")));
    }
}
