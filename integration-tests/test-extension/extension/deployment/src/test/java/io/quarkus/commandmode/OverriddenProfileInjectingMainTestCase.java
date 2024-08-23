package io.quarkus.commandmode;

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

public class OverriddenProfileInjectingMainTestCase {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PropertyInjectingMain.class))
            .setApplicationName("property-injecting")
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("test.message", "hello from ${quarkus.profile}")
            .setRuntimeProperties(Collections.singletonMap("quarkus.profile", "other"))
            .setExpectExit(true)
            .setRun(true);

    @Test
    public void testRun() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("hello from other").contains("other activated");
        Assertions.assertThat(config.getExitCode()).isEqualTo(0);
    }

}
