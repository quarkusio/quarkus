package io.quarkus.aesh.deployment;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that CDI injection works correctly in aesh commands.
 * A command with {@code @CommandDefinition} and {@code @Inject} should
 * have its dependencies injected by the CDI container.
 */
public class CdiInjectionInCommandTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    CdiInjectionCommand.class,
                    GreetingService.class))
            .setApplicationName("cdi-inject-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--name=Quarkus");

    @Test
    public void testCdiInjectionInCommand() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("Hello Quarkus from CDI!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }
}
