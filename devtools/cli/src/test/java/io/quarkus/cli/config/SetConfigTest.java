package io.quarkus.cli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

class SetConfigTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path resources = tempDir.resolve("src/main/resources");
        Files.createDirectories(resources);
        Files.createFile(resources.resolve("application.properties"));
    }

    @Test
    void addConfiguration() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "set", "foo.bar", "1234");
        assertEquals(0, result.getExitCode());
        assertEquals("1234", config().getRawValue("foo.bar"));
    }

    @Test
    void setConfiguration() throws Exception {
        Path propertiesFile = tempDir.resolve("src/main/resources/application.properties");
        Properties properties = new Properties();
        try (InputStream inputStream = propertiesFile.toUri().toURL().openStream()) {
            properties.load(inputStream);
        }
        properties.put("foo.bar", "1234");
        try (FileOutputStream outputStream = new FileOutputStream(propertiesFile.toFile())) {
            properties.store(outputStream, "");
        }
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "set", "foo.bar", "5678");
        assertEquals(0, result.getExitCode());
        assertEquals("5678", config().getRawValue("foo.bar"));
    }

    @Test
    void addEncryptedConfiguration() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "set", "foo.bar", "1234", "-k");
        assertEquals(0, result.getExitCode());

        SmallRyeConfig config = config();
        assertEquals("1234", config.getConfigValue("foo.bar").getValue());

        String encryption = config.getRawValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key");
        assertNotNull(encryption);

        result = CliDriver.execute(tempDir, "config", "set", "foo.baz", "5678", "-k");
        assertEquals(0, result.getExitCode());

        config = config();

        assertEquals("1234", config.getConfigValue("foo.bar").getValue());
        assertTrue(config.isPropertyPresent("foo.baz"));
        assertEquals("5678", config.getConfigValue("foo.baz").getValue());
    }

    @Test
    void setEncryptedConfiguration() throws Exception {
        Path propertiesFile = tempDir.resolve("src/main/resources/application.properties");
        Properties properties = new Properties();
        try (InputStream inputStream = propertiesFile.toUri().toURL().openStream()) {
            properties.load(inputStream);
        }
        properties.put("foo.bar", "1234");
        try (FileOutputStream outputStream = new FileOutputStream(propertiesFile.toFile())) {
            properties.store(outputStream, "");
        }

        CliDriver.Result result = CliDriver.execute(tempDir, "config", "set", "foo.bar", "-k");
        assertEquals(0, result.getExitCode());

        SmallRyeConfig config = config();
        assertEquals("1234", config.getConfigValue("foo.bar").getValue());
    }

    private SmallRyeConfig config() throws Exception {
        PropertiesConfigSource propertiesConfigSource = new PropertiesConfigSource(
                tempDir.resolve("src/main/resources/application.properties").toUri().toURL());
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withSources(propertiesConfigSource)
                .withDefaultValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "default")
                .build();
    }
}
