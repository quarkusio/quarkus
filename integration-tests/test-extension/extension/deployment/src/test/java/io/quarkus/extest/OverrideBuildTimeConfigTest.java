package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class OverrideBuildTimeConfigTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar
                    .addClass(OverrideBuildTimeConfigSource.class)
                    .addAsServiceProvider(ConfigSource.class, OverrideBuildTimeConfigSource.class))
            .setRuntimeProperties(Map.of(
                    "quarkus.application.name", "foo",
                    "quarkus.mapping.btrt.optional", "value",
                    "quarkus.mapping.btrt.unlisted", "value"))
            .setRun(true);

    @Test
    void overrideBuildTimeConfigTest() {
        assertTrue(TEST.getStartupConsoleOutput()
                .contains("- quarkus.application.name is set to 'foo' but it is build time fixed to " +
                        "'quarkus-integration-test-test-extension-extension-deployment'."));
        assertTrue(TEST.getStartupConsoleOutput()
                .contains("- quarkus.mapping.btrt.optional is set to 'value' but it is build time fixed to 'null'."));
        assertFalse(TEST.getStartupConsoleOutput()
                .contains("- quarkus.mapping.btrt.unlisted is set to 'value' but it is build time fixed to 'null'."));
    }
}
