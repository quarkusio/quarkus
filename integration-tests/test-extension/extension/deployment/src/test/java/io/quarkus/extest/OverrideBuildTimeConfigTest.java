package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class OverrideBuildTimeConfigTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .setRuntimeProperties(Map.of("quarkus.application.name", "foo", "quarkus.mapping.btrt.optional", "value"))
            .setRun(true);

    @Test
    void overrideBuildTimeConfigTest() {
        assertTrue(TEST.getStartupConsoleOutput()
                .contains("- quarkus.application.name is set to 'foo' but it is build time fixed to " +
                        "'quarkus-integration-test-test-extension-extension-deployment'."));
        assertTrue(TEST.getStartupConsoleOutput()
                .contains("- quarkus.mapping.btrt.optional is set to 'value' but it is build time fixed to 'null'."));
    }
}
