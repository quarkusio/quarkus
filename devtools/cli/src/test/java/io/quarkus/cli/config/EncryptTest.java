package io.quarkus.cli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.cli.CliDriver;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Parsing the stdout is not working on Github Windows, maybe because of the console formatting. "
        +
        "I did try it in a Windows box and it works fine. Regardless, this commands is tested indirectly" +
        " in SetConfigTest, which is still enabled in Windows ")
class EncryptTest {
    @TempDir
    Path tempDir;

    @Test
    void encrypt() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "encrypt", "12345678");
        Scanner scanner = new Scanner(result.getStdout());
        String[] split = scanner.nextLine().split(" ");
        String secret = split[split.length - 8];
        String encryptionKey = split[split.length - 1];

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
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "encrypt", "12345678", "-f=plain",
                "--key=12345678");
        Scanner scanner = new Scanner(result.getStdout());
        String[] split = scanner.nextLine().split(" ");
        String secret = split[split.length - 1];

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
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "encrypt", "12345678", "--key=12345678");
        Scanner scanner = new Scanner(result.getStdout());
        String[] split = scanner.nextLine().split(" ");
        String secret = split[split.length - 1];

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .addDiscoveredSecretKeysHandlers()
                .withDefaultValue("my.secret", "${aes-gcm-nopadding::" + secret + "}")
                .withDefaultValue("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "MTIzNDU2Nzg")
                .build();

        assertEquals("12345678", config.getConfigValue("my.secret").getValue());
    }
}
