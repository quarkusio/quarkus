package io.quarkus.extest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class OverrideBuildTimeConfigTest {
    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .setRuntimeProperties(Map.of("quarkus.tls.trust-all", "true"))
            .setRun(true);

    @Test
    void overrideBuildTimeConfigTest() {
        assertTrue(TEST.getStartupConsoleOutput()
                .contains(
                        "- quarkus.tls.trust-all is set to 'true' but it is build time fixed to 'false'. Did you " +
                                "change the property quarkus.tls.trust-all after building the application?"));
    }
}
