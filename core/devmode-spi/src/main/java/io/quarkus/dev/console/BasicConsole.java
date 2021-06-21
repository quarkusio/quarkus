package io.quarkus.dev.console;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BasicConsole extends QuarkusConsole {

    private static final Logger log = Logger.getLogger(BasicConsole.class.getName());
    private static final Logger statusLogger = Logger.getLogger("quarkus");

    private static final ThreadLocal<Boolean> DISABLE_FILTER = new ThreadLocal<>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    final PrintStream printStream;
    final boolean inputSupport;
    final boolean noColor;

    public BasicConsole(boolean noColor, boolean inputSupport, PrintStream printStream) {
        this.noColor = noColor;
        this.inputSupport = inputSupport;
        this.printStream = printStream;
        if (inputSupport) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            int val = System.in.read();
                            if (val == -1) {
                                return;
                            }
                            InputHolder handler = inputHandlers.peek();
                            if (handler != null) {
                                handler.handler.handleInput(new int[] { val });
                            }
                        } catch (IOException e) {
                            log.log(Level.SEVERE, "Failed to read user input", e);
                            return;
                        }
                    }

                }
            }, "Quarkus Terminal Reader");
            t.setDaemon(true);
            t.start();
        }
    }

    @Override
    public InputHolder createHolder(InputHandler inputHandler) {
        return new InputHolder(inputHandler) {
            @Override
            protected void setPromptMessage(String prompt) {
                if (!inputSupport) {
                    return;
                }
                if (prompt == null) {
                    return;
                }
                DISABLE_FILTER.set(true);
                try {
                    statusLogger.info(prompt);
                } finally {
                    DISABLE_FILTER.set(false);
                }
            }

            @Override
            protected void setResultsMessage(String results) {
                DISABLE_FILTER.set(true);
                try {
                    statusLogger.info(results);
                } finally {
                    DISABLE_FILTER.set(false);
                }
            }

            @Override
            protected void setCompileErrorMessage(String results) {
                DISABLE_FILTER.set(true);
                try {
                    statusLogger.info(results);
                } finally {
                    DISABLE_FILTER.set(false);
                }
            }

            @Override
            protected void setStatusMessage(String status) {
                if (status == null) {
                    return;
                }
                DISABLE_FILTER.set(true);
                try {
                    statusLogger.info(status);
                } finally {
                    DISABLE_FILTER.set(false);
                }
            }
        };
    }

    @Override
    public void write(String s) {
        if (outputFilter != null) {
            if (!outputFilter.test(s)) {
                //we still test, the output filter may be recording output
                if (!DISABLE_FILTER.get()) {
                    return;
                }
            }
        }
        if (noColor || !hasColorSupport()) {
            printStream.print(stripAnsiCodes(s));
        } else {
            printStream.print(s);
        }

    }

    @Override
    public void write(byte[] buf, int off, int len) {
        write(new String(buf, off, len, Charset.defaultCharset()));
    }

}
