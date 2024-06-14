package io.quarkus.cli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.cli.CliDriver;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class RemoveConfigTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path resources = tempDir.resolve("src/main/resources");
        Files.createDirectories(resources);
        Files.createFile(resources.resolve("application.properties"));
    }

    @Test
    void removeConfiguration() throws Exception {
        Path propertiesFile = tempDir.resolve("src/main/resources/application.properties");
        Properties properties = new Properties();
        try (InputStream inputStream = propertiesFile.toUri().toURL().openStream()) {
            properties.load(inputStream);
        }
        properties.put("foo.bar", "1234");
        try (FileOutputStream outputStream = new FileOutputStream(propertiesFile.toFile())) {
            properties.store(outputStream, "");
        }
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "remove", "foo.bar");
        System.out.println(result.getStdout());
        assertEquals(0, result.getExitCode());
        assertTrue(config().getOptionalValue("foo.bar", String.class).isEmpty());
    }

    private SmallRyeConfig config() throws Exception {
        PropertiesConfigSource propertiesConfigSource = new PropertiesConfigSource(
                tempDir.resolve("src/main/resources/application.properties").toUri().toURL());
        return new SmallRyeConfigBuilder()
                .withSources(propertiesConfigSource)
                .build();
    }
}
