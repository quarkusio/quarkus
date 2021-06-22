package io.quarkus.dev.console;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.function.Predicate;

public abstract class QuarkusConsole {

    public static final String LAUNCHED_FROM_IDE = "io.quarkus.launched-from-ide";

    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

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
    protected final ArrayDeque<InputHolder> inputHandlers = new ArrayDeque<>();

    public static volatile QuarkusConsole INSTANCE = new BasicConsole(false, false, System.out);

    public static volatile boolean installed;

    protected volatile Predicate<String> outputFilter;

    private volatile boolean started = false;

    public static boolean hasColorSupport() {
        if (Boolean.getBoolean(LAUNCHED_FROM_IDE)) {
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
            return System.console() != null;
        }
    }

    public synchronized void pushInputHandler(InputHandler inputHandler) {
        InputHolder holder = inputHandlers.peek();
        if (holder != null) {
            holder.setEnabled(false);
        }
        holder = createHolder(inputHandler);
        inputHandler.promptHandler(holder);
        if (started) {
            holder.setEnabled(true);
        }
        inputHandlers.push(holder);
    }

    public synchronized void popInputHandler() {
        InputHolder holder = inputHandlers.pop();
        holder.setEnabled(false);
        holder = inputHandlers.peek();
        if (holder != null) {
            holder.setEnabled(true);
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
        InputHolder holder = inputHandlers.peek();
        if (holder != null) {
            holder.setEnabled(true);
        }
    }

    public abstract InputHolder createHolder(InputHandler inputHandler);

    public abstract void write(String s);

    public abstract void write(byte[] buf, int off, int len);

    protected String stripAnsiCodes(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "");
        return s;
    }

    public void setOutputFilter(Predicate<String> logHandler) {
        this.outputFilter = logHandler;
    }

    protected static abstract class InputHolder implements InputHandler.ConsoleStatus {
        public final InputHandler handler;
        volatile boolean enabled;
        String prompt;
        String status;
        String results;
        String compileError;

        protected InputHolder(InputHandler handler) {
            this.handler = handler;
        }

        public InputHolder setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (enabled) {
                setStatus(status);
                setPrompt(prompt);
                setResults(results);
                setCompileError(compileError);
            }
            return this;
        }

        @Override
        public void setPrompt(String prompt) {
            this.prompt = prompt;
            if (enabled) {
                setPromptMessage(prompt);
            }
        }

        @Override
        public void setStatus(String status) {
            this.status = status;
            if (enabled) {
                setStatusMessage(status);
            }
        }

        @Override
        public void setResults(String results) {
            this.results = results;
            if (enabled) {
                setResultsMessage(results);
            }
        }

        @Override
        public void setCompileError(String compileError) {
            this.compileError = compileError;
            if (enabled) {
                setCompileErrorMessage(compileError);
            }
        }

        protected abstract void setStatusMessage(String status);

        protected abstract void setPromptMessage(String prompt);

        protected abstract void setResultsMessage(String results);

        protected abstract void setCompileErrorMessage(String results);
    }
}
