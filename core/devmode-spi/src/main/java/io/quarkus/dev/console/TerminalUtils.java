package io.quarkus.dev.console;

import java.io.Console;

import org.jboss.logging.Logger;

import io.smallrye.common.os.OS;

public class TerminalUtils {

    // Make fully private as soon as we drop the constants in QuarkusConsole
    static final String FORCE_COLOR_SUPPORT = "io.quarkus.force-color-support";

    /**
     * <a href="https://conemu.github.io">ConEmu</a> ANSI X3.64 support enabled,
     * used by <a href="https://cmder.net/">cmder</a>
     */
    // Make fully private as soon as we drop the constants in QuarkusConsole
    static final boolean IS_CON_EMU_ANSI = OS.WINDOWS.isCurrent() && "ON".equals(System.getenv("ConEmuANSI"));

    /**
     * These tests are same as used in jansi
     * Source: https://github.com/fusesource/jansi/commit/bb3d538315c44f799d34fd3426f6c91c8e8dfc55
     */
    // Make fully private as soon as we drop the constants in QuarkusConsole
    static final boolean IS_CYGWIN = OS.WINDOWS.isCurrent()
            && System.getenv("PWD") != null
            && System.getenv("PWD").startsWith("/")
            && !"cygwin".equals(System.getenv("TERM"));

    // Make fully private as soon as we drop the constants in QuarkusConsole
    static final boolean IS_MINGW_XTERM = OS.WINDOWS.isCurrent()
            && System.getenv("MSYSTEM") != null
            && System.getenv("MSYSTEM").startsWith("MINGW")
            && "xterm".equals(System.getenv("TERM"));

    public static boolean hasColorSupport() {
        checkAndSetJdkConsole();
        if (Boolean.getBoolean(FORCE_COLOR_SUPPORT)) {
            return true; //assume the IDE run window has color support
        }
        if (OS.WINDOWS.isCurrent()) {
            // On Windows without a known good emulator
            // TODO: optimally we would check if Win32 getConsoleMode has
            // ENABLE_VIRTUAL_TERMINAL_PROCESSING enabled or enable it via
            // setConsoleMode.
            // For now we turn it off to not generate noisy output for most
            // users.
            // Must be on some Unix variant or ANSI-enabled windows terminal...
            return IS_CON_EMU_ANSI || IS_CYGWIN || IS_MINGW_XTERM;
        } else {
            // on sane operating systems having a console is a good indicator
            // you are attached to a TTY with colors.

            return TerminalUtils.isTerminal(System.console());
        }
    }

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
            // we don't make it a static as it's only used for an error case
            Logger.getLogger(TerminalUtils.class.getName())
                    .error("Failed to invoke System.console().isTerminal() via Reflection API", e);
            return false;
        }
    }

    private static void checkAndSetJdkConsole() {
        // the JLine console in JDK 23+ causes significant startup slowdown,
        // so we avoid it unless the user opted into it
        String res = System.getProperty("jdk.console");
        if (res == null) {
            System.setProperty("jdk.console", "java.base");
        }
    }
}
