package io.quarkus.registry.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ImmutableRegistriesConfigTest {
    static Path baseDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("src/test/resources/devtools-config");
    static Path writeDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-serialization");

    @BeforeAll
    static void verifyPath() throws IOException {
        Assertions.assertTrue(Files.exists(baseDir), baseDir + " should exist");
        Files.createDirectories(writeDir);
    }

    @Test
    public void testImmutableRegistriesConfig() throws IOException {
        Files.list(baseDir)
                .forEach(f -> {
                    System.out.println("testing " + f);
                    Path configYaml = baseDir.resolve(f);
                    try {
                        String fileName = f.getFileName().toString();
                        System.out.println(fileName);
                        Path tmp = writeDir.resolve(fileName);
                        RegistriesConfig rConfig = RegistriesConfigMapperHelper.deserialize(configYaml,
                                RegistriesConfigImpl.class);
                        RegistriesConfigMapperHelper.serialize(rConfig, tmp);
                    } catch (IOException e) {
                        System.out.println("failed " + f + " due to " + e);
                        Assertions.fail(e);
                    }
                });
    }
}
