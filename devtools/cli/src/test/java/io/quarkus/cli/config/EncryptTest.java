package io.quarkus.cli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.cli.CliDriver;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class EncryptTest {
    @TempDir
    Path tempDir;

    @Test
    void encrypt() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "encrypt", "--secret=12345678");
        Scanner scanner = new Scanner(result.getStdout());
        String secret = scanner.nextLine().split(": ")[1];
        String encryptionKey = scanner.nextLine().split(": ")[1];

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${aes-gcm-nopadding::" + secret + "}")
                .withDefaultValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", encryptionKey)
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void keyPlain() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "encrypt", "--secret=12345678", "-f=plain",
                "--key=12345678");
        Scanner scanner = new Scanner(result.getStdout());
        String secret = scanner.nextLine().split(": ")[1];

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${aes-gcm-nopadding::" + secret + "}")
                .withDefaultValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "12345678")
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());

        config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${aes-gcm-nopadding::" + secret + "}")
                .withDefaultValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "MTIzNDU2Nzg")
                .withDefaultValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key-decode", "true")
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());
    }

    @Test
    void keyBase64() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "encrypt", "--secret=12345678", "--key=12345678");
        Scanner scanner = new Scanner(result.getStdout());
        String secret = scanner.nextLine().split(": ")[1];

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${aes-gcm-nopadding::" + secret + "}")
                .withDefaultValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "MTIzNDU2Nzg")
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());
    }
}
