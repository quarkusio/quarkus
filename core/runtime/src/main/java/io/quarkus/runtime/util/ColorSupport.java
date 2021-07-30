package io.quarkus.runtime.util;

import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.ConsoleConfig;

public class ColorSupport {

    public static boolean isColorEnabled(ConsoleRuntimeConfig consoleConfig, ConsoleConfig logConfig) {
        if (consoleConfig.color.isPresent()) {
            return consoleConfig.color.get();
        }
        if (logConfig.color.isPresent()) {
            return logConfig.color.get();
        }
        return QuarkusConsole.hasColorSupport();
    }
}
