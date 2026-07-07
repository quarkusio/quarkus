package io.quarkus.gradle.application.internal.launch;

import org.gradle.api.logging.configuration.ConsoleOutput;

public final class ConsoleColorSupport {

    public static final String FORCE_COLOR_SUPPORT_PROPERTY = "io.quarkus.force-color-support";

    private ConsoleColorSupport() {
    }

    public static boolean forcePlainConsole(ConsoleOutput consoleOutput, String noColor) {
        return consoleOutput == ConsoleOutput.Plain || noColor != null && !noColor.isEmpty();
    }

    public static String jvmArgument(boolean forcePlainConsole, String configuredValue) {
        boolean forceColorSupport = configuredValue == null
                ? !forcePlainConsole
                : Boolean.parseBoolean(configuredValue);
        return "-D" + FORCE_COLOR_SUPPORT_PROPERTY + "=" + forceColorSupport;
    }
}
