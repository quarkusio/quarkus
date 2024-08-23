package io.quarkus.dev.console;

import java.io.Console;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TerminalUtils {

    private static final Logger LOGGER = Logger.getLogger(TerminalUtils.class.getName());

    public static boolean isTerminal(Console console) {
        if (console == null) {
            return false;
        }

        if (Runtime.version().feature() < 22) { // isTerminal was introduced in Java 22
            return true;
        }

        try {
            return (boolean) Console.class.getMethod("isTerminal").invoke(console);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to invoke System.console().isTerminal() via Reflection API", e);
            return false;
        }
    }
}
