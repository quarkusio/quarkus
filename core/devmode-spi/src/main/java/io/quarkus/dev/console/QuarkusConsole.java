package io.quarkus.dev.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public abstract class QuarkusConsole {

    public static final int TEST_STATUS = 100;
    public static final int TEST_RESULTS = 200;
    public static final int COMPILE_ERROR = 300;

    public static final String FORCE_COLOR_SUPPORT = "io.quarkus.force-color-support";

    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");
    public static final boolean IS_MAC = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac");
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("linux");

    /**
     * <a href="https://conemu.github.io">ConEmu</a> ANSI X3.64 support enabled,
     * used by <a href="https://cmder.net/">cmder</a>
     */
    public static final boolean IS_CON_EMU_ANSI = IS_WINDOWS && "ON".equals(System.getenv("ConEmuANSI"));

    /**
     * These tests are same as used in jansi
     * Source: https://github.com/fusesource/jansi/commit/bb3d538315c44f799d34fd3426f6c91c8e8dfc55
     */
    public static final boolean IS_CYGWIN = IS_WINDOWS
            && System.getenv("PWD") != null
            && System.getenv("PWD").startsWith("/")
            && !"cygwin".equals(System.getenv("TERM"));

    public static final boolean IS_MINGW_XTERM = IS_WINDOWS
            && System.getenv("MSYSTEM") != null
            && System.getenv("MSYSTEM").startsWith("MINGW")
            && "xterm".equals(System.getenv("TERM"));
    protected volatile Consumer<int[]> inputHandler;

    public static volatile QuarkusConsole INSTANCE = new BasicConsole(hasColorSupport(), false, System.out::print);

    public static volatile boolean installed;

    protected static final List<BiPredicate<String, Boolean>> outputFilters = new CopyOnWriteArrayList<>();

    private volatile boolean started = false;

    static boolean redirectsInstalled = false;

    public final static PrintStream ORIGINAL_OUT = System.out;
    public final static PrintStream ORIGINAL_ERR = System.err;
    public final static InputStream ORIGINAL_IN = System.in;

    public static PrintStream REDIRECT_OUT = null;
    public static PrintStream REDIRECT_ERR = null;
    public static StateChangeInputStream REDIRECT_IN;
    protected volatile boolean userReadInProgress;

    public synchronized static void installRedirects() {
        if (redirectsInstalled) {
            return;
        }
        redirectsInstalled = true;

        //force console init
        //otherwise you can get a stack overflow as it sees the redirected output
        QuarkusConsole.INSTANCE.isInputSupported();
        REDIRECT_OUT = new RedirectPrintStream(false);
        REDIRECT_ERR = new RedirectPrintStream(true);
        REDIRECT_IN = new StateChangeInputStream();
        System.setOut(REDIRECT_OUT);
        System.setErr(REDIRECT_ERR);
        System.setIn(REDIRECT_IN);
    }

    public synchronized static void uninstallRedirects() {
        if (!redirectsInstalled) {
            return;
        }

        if (REDIRECT_OUT != null) {
            REDIRECT_OUT.flush();
            REDIRECT_OUT.close();
            REDIRECT_OUT = null;
        }
        if (REDIRECT_ERR != null) {
            REDIRECT_ERR.flush();
            REDIRECT_ERR.close();
            REDIRECT_ERR = null;
        }
        REDIRECT_IN = null;
        System.setOut(ORIGINAL_OUT);
        System.setErr(ORIGINAL_ERR);
        System.setIn(ORIGINAL_IN);

        redirectsInstalled = false;
    }

    private static void checkAndSetJdkConsole() {
        // the JLine console in JDK 23+ causes significant startup slowdown,
        // so we avoid it unless the user opted into it
        String res = System.getProperty("jdk.console");
        if (res == null) {
            System.setProperty("jdk.console", "java.base");
        }
    }

    public static boolean hasColorSupport() {
        checkAndSetJdkConsole();
        if (Boolean.getBoolean(FORCE_COLOR_SUPPORT)) {
            return true; //assume the IDE run window has color support
        }
        if (IS_WINDOWS) {
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

    public static void start() {
        INSTANCE.startInternal();
    }

    private synchronized void startInternal() {
        if (started) {
            return;
        }
        started = true;
    }

    public void setInputHandler(Consumer<int[]> inputHandler) {
        this.inputHandler = inputHandler;
    }

    public abstract void doReadLine();

    public abstract StatusLine registerStatusLine(int priority);

    public abstract void setPromptMessage(String message);

    public abstract void write(boolean errorStream, String s);

    public abstract void write(boolean errorStream, byte[] buf, int off, int len);

    public void exitCliMode() {
        //noop for the non-aesh console
    }

    /**
     * Exposes single character aliases so they can be displayed in the help screen
     */
    public Map<Character, String> singleLetterAliases() {
        return Map.of();
    }

    /**
     * runs a single letter alias
     */
    public void runAlias(char alias) {

    }

    protected String stripAnsiCodes(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "");
        return s;
    }

    public static void addOutputFilter(BiPredicate<String, Boolean> logHandler) {
        outputFilters.add(logHandler);
    }

    public static void removeOutputFilter(BiPredicate<String, Boolean> logHandler) {
        outputFilters.remove(logHandler);
    }

    protected boolean shouldWrite(boolean errorStream, String s) {
        boolean ok = true;
        for (var i : outputFilters) {
            if (!i.test(s, errorStream)) {
                //no early exit as filters can also record output
                ok = false;
            }
        }
        return ok;
    }

    public boolean isInputSupported() {
        return true;
    }

    public boolean isAnsiSupported() {
        return false;
    }

    protected void userReadStart() {

    }

    protected void userReadStop() {

    }

    public static class StateChangeInputStream extends InputStream {

        private final LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<>();

        private volatile boolean reading;

        public synchronized boolean acceptInput(int input) {
            if (reading) {
                queue.add(input);
                notifyAll();
                return true;
            }
            return false;
        }

        @Override
        public synchronized int read() throws IOException {
            reading = true;
            try {
                while (queue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException();
                    }
                }
                return queue.pollFirst();
            } finally {
                reading = false;
            }
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            reading = true;
            int read = 0;
            try {
                while (read < len) {
                    while (queue.isEmpty()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new InterruptedIOException();
                        }
                    }
                    byte byteValue = queue.poll().byteValue();
                    b[read++] = byteValue;
                    if (byteValue == '\n' || byteValue == '\r') {
                        return read;
                    }
                }
                return read;
            } finally {
                reading = false;
            }
        }
    }
}
