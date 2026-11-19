package io.quarkus.deployment.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;

public class ConsoleProcessorTest {

    @Test
    public void testConsoleInitializesWhenDisplayDisabled() throws Exception {
        Field installedField = ConsoleProcessor.class.getDeclaredField("consoleInstalled");
        installedField.setAccessible(true);

        boolean previousInstalled = installedField.getBoolean(null);
        boolean previousInitialized = ConsoleStateManager.initialized;
        boolean previousConsoleInstalled = QuarkusConsole.installed;
        QuarkusConsole previousConsole = QuarkusConsole.INSTANCE;

        try {
            installedField.setBoolean(null, false);
            ConsoleStateManager.initialized = false;
            QuarkusConsole.installed = false;

            ConsoleConfig config = new ConsoleConfig() {
                public boolean enabled() {
                    return false;
                }

                public boolean disableInput() {
                    return false;
                }

                public boolean basic() {
                    return true;
                }

                public Optional<Boolean> color() {
                    return Optional.of(false);
                }
            };

            LaunchModeBuildItem launchMode = new LaunchModeBuildItem(
                    LaunchMode.DEVELOPMENT,
                    Optional.of(DevModeType.LOCAL),
                    false,
                    Optional.empty(),
                    true);

            new ConsoleProcessor().setupConsole(null, config, launchMode, null);

            assertThat(ConsoleStateManager.initialized).isTrue();
        } finally {
            installedField.setBoolean(null, previousInstalled);
            ConsoleStateManager.initialized = previousInitialized;
            QuarkusConsole.installed = previousConsoleInstalled;
            QuarkusConsole.INSTANCE = previousConsole;
        }
    }
}
