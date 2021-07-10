package io.quarkus.dev.console;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    final Consumer<String> output;
    final Supplier<Integer> input;
    final boolean inputSupport;
    final boolean color;

    volatile boolean readingLine;

    public BasicConsole(boolean color, boolean inputSupport, Consumer<String> output) {
        this(color, inputSupport, output, () -> {
            try {
                return System.in.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public BasicConsole(boolean color, boolean inputSupport, Consumer<String> output, Supplier<Integer> inputProvider) {
        this.color = color;
        this.inputSupport = inputSupport;
        this.output = output;
        this.input = inputProvider;
        if (inputSupport) {
            Objects.requireNonNull(inputProvider);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            int val = input.get();
                            if (val == -1) {
                                return;
                            }
                            if (readingLine) {
                                //when doing a read line we want to discard the first \n
                                //as this was the one that was needed to activate this mode
                                if (val == '\n') {
                                    readingLine = false;
                                    continue;
                                }
                            }
                            InputHolder handler = inputHandlers.peek();
                            if (handler != null) {
                                handler.handler.handleInput(new int[] { val });
                            }
                        } catch (Exception e) {
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
            public void doReadLine() {
                readingLine = true;
                output.accept(">");
            }

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
                    System.out.println(prompt);
                } finally {
                    DISABLE_FILTER.set(false);
                }
            }

            @Override
            protected void setResultsMessage(String results) {
                if (results == null) {
                    return;
                }
                DISABLE_FILTER.set(true);
                try {
                    System.out.println(results);
                } finally {
                    DISABLE_FILTER.set(false);
                }
            }

            @Override
            protected void setCompileErrorMessage(String results) {
                if (results == null) {
                    return;
                }
                DISABLE_FILTER.set(true);
                try {
                    System.out.println(results);
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
                    System.out.println(status);
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
        if (!color) {
            output.accept(stripAnsiCodes(s));
        } else {
            output.accept(s);
        }

    }

    @Override
    public void write(byte[] buf, int off, int len) {
        write(new String(buf, off, len, StandardCharsets.UTF_8));
    }

}
