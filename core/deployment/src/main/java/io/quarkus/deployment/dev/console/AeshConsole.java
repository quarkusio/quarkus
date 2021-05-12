package io.quarkus.deployment.dev.console;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.ANSI;

import io.quarkus.dev.console.InputHandler;
import io.quarkus.dev.console.QuarkusConsole;

public class AeshConsole extends QuarkusConsole {

    private final Connection connection;
    private final boolean inputSupport;
    private Size size;
    private Attributes attributes;

    private String statusMessage;
    private String promptMessage;
    private int totalStatusLines = 0;
    private int lastWriteCursorX;
    /**
     * The write queue
     * <p>
     * Data must be added to this, before it is written out by {@link #deadlockSafeWrite()}
     * <p>
     * Because Aesh can log deadlocks are possible on windows if a write fails, unless care
     * is taken.
     */
    private final LinkedBlockingDeque<String> writeQueue = new LinkedBlockingDeque<>();
    private final Lock connectionLock = new ReentrantLock();
    private static final ThreadLocal<Boolean> IN_WRITE = new ThreadLocal<>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public AeshConsole(Connection connection, boolean inputSupport) {
        this.inputSupport = inputSupport;
        INSTANCE = this;
        this.connection = connection;
        connection.openNonBlocking();
        setup(connection);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                connection.close();
            }
        }, "Console Shutdown Hoot"));
    }

    private AeshConsole setStatusMessage(String statusMessage) {
        synchronized (this) {
            StringBuilder buffer = new StringBuilder();
            clearStatusMessages(buffer);
            int newLines = countLines(statusMessage) + countLines(promptMessage);
            if (statusMessage == null) {
                if (promptMessage != null) {
                    newLines += 2;
                }
            } else if (promptMessage == null) {
                newLines += 2;
            } else {
                newLines += 3;
            }
            if (newLines > totalStatusLines) {
                for (int i = 0; i < newLines - totalStatusLines; ++i) {
                    buffer.append("\n");
                }
            }
            this.statusMessage = statusMessage;
            this.totalStatusLines = newLines;
            printStatusAndPrompt(buffer);
            writeQueue.add(buffer.toString());
        }
        deadlockSafeWrite();
        return this;
    }

    public AeshInputHolder createHolder(InputHandler inputHandler) {
        return new AeshInputHolder(inputHandler);
    }

    private AeshConsole setPromptMessage(String promptMessage) {
        synchronized (this) {
            if (!inputSupport) {
                return this;
            }
            StringBuilder buffer = new StringBuilder();
            clearStatusMessages(buffer);
            int newLines = countLines(statusMessage) + countLines(promptMessage);
            if (statusMessage == null) {
                if (promptMessage != null) {
                    newLines += 2;
                }
            } else if (promptMessage == null) {
                newLines += 2;
            } else {
                newLines += 3;
            }
            if (newLines > totalStatusLines) {
                for (int i = 0; i < newLines - totalStatusLines; ++i) {
                    buffer.append("\n");
                }
            }
            this.promptMessage = promptMessage;
            this.totalStatusLines = newLines;
            printStatusAndPrompt(buffer);
            writeQueue.add(buffer.toString());
        }
        deadlockSafeWrite();
        return this;
    }

    private void end(Connection conn) {
        conn.setAttributes(attributes);
        StringBuilder sb = new StringBuilder();
        sb.append(ANSI.MAIN_BUFFER);
        sb.append(ANSI.CURSOR_SHOW);
        sb.append("\u001B[0m");
        writeQueue.add(sb.toString());
        deadlockSafeWrite();
    }

    private void deadlockSafeWrite() {
        for (;;) {
            //after we have unlocked we always need to check again
            //another thread may have added something to the queue after our last write but before
            //we unlocked. Checking again makes sure we are safe
            if (writeQueue.isEmpty()) {
                return;
            }
            if (connectionLock.tryLock()) {
                //we need to guard against Aesh logging something if there is a problem
                //it results in an infinite loop otherwise
                IN_WRITE.set(true);
                try {
                    while (!writeQueue.isEmpty()) {
                        String s = writeQueue.poll();
                        connection.write(s);
                    }
                } finally {
                    IN_WRITE.set(false);
                    connectionLock.unlock();
                }
            }
        }
    }

    private void setup(Connection conn) {
        synchronized (this) {
            size = conn.size();
            if (inputSupport) {
                conn.setSignalHandler(event -> {
                    switch (event) {
                        case INT:
                            //todo: why does async exit not work here
                            //Quarkus.asyncExit();
                            //end(conn);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    System.exit(0);
                                }
                            }).start();
                            break;
                    }
                });
                // Keyboard handling
                conn.setStdinHandler(keys -> {
                    InputHolder handler = inputHandlers.peek();
                    if (handler != null) {
                        handler.handler.handleInput(keys);
                    }
                });
            }
            conn.setCloseHandler(close -> end(conn));
            conn.setSizeHandler(size -> setup(conn));

            if (inputSupport) {
                attributes = conn.enterRawMode();
            } else {
                attributes = conn.getAttributes();
            }

            StringBuilder sb = new StringBuilder();
            printStatusAndPrompt(sb);
            writeQueue.add(sb.toString());
        }
        deadlockSafeWrite();
    }

    /**
     * prints the status messages
     * <p>
     * this will overwrite the bottom part of the screen
     * callers are responsible for writing enough newlines to
     * preserve any console history they want.
     *
     * @param buffer
     */
    private void printStatusAndPrompt(StringBuilder buffer) {
        if (totalStatusLines == 0) {
            return;
        }

        clearStatusMessages(buffer);
        gotoLine(buffer, size.getHeight() - totalStatusLines);
        buffer.append("\n--\n");
        if (statusMessage != null) {
            buffer.append(statusMessage);
            if (promptMessage != null) {
                buffer.append("\n");
            }
        }
        if (promptMessage != null) {
            buffer.append(promptMessage);
        }
    }

    private void clearStatusMessages(StringBuilder buffer) {
        gotoLine(buffer, size.getHeight() - totalStatusLines);
        buffer.append("\033[J");
    }

    private StringBuilder gotoLine(StringBuilder builder, int line) {
        return builder.append("\033[").append(line).append(";").append(0).append("H");
    }

    int countLines(String s) {
        return countLines(s, 0);
    }

    int countLines(String s, int cursorPos) {
        if (s == null) {
            return 0;
        }
        s = stripAnsiCodes(s);
        int lines = 0;
        int curLength = cursorPos;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == '\n') {
                lines++;
                curLength = 0;
            } else if (curLength++ == size.getWidth()) {
                lines++;
                curLength = 0;
            }
        }
        return lines;
    }

    public void write(String s) {
        if (IN_WRITE.get()) {
            return;
        }
        StringBuilder buffer = new StringBuilder();
        synchronized (this) {
            if (outputFilter != null) {
                if (!outputFilter.test(s)) {
                    return;
                }
            }
            if (totalStatusLines == 0) {
                writeQueue.add(s);
            } else {
                clearStatusMessages(buffer);
                int cursorPos = lastWriteCursorX;
                gotoLine(buffer, size.getHeight());
                String stripped = stripAnsiCodes(s);
                int lines = countLines(s, cursorPos);
                int trailing = 0;
                int index = stripped.lastIndexOf("\n");
                if (index == -1) {
                    trailing = stripped.length();
                } else {
                    trailing = stripped.length() - index - 1;
                }

                int newCursorPos;
                if (lines == 0) {
                    newCursorPos = trailing + cursorPos;
                } else {
                    newCursorPos = trailing;
                }

                if (cursorPos > 1 && lines == 0) {
                    buffer.append(s);
                    lastWriteCursorX = newCursorPos;
                    //partial line, just write it
                    connection.write(buffer.toString());
                    return;
                }
                if (lines == 0) {
                    lines++;
                }
                //move the existing content up by the number of lines
                int appendLines = Math.max(Math.min(cursorPos > 1 ? lines - 1 : lines, totalStatusLines), 1);
                clearStatusMessages(buffer);
                buffer.append("\033[").append(size.getHeight() - totalStatusLines).append(";").append(0).append("H");
                buffer.append(s);
                buffer.append("\033[").append(size.getHeight()).append(";").append(0).append("H");
                for (int i = 0; i < appendLines; ++i) {
                    buffer.append("\n");
                }
                lastWriteCursorX = newCursorPos;
                printStatusAndPrompt(buffer);
                writeQueue.add(buffer.toString());
            }
        }
        deadlockSafeWrite();
    }

    public void write(byte[] buf, int off, int len) {
        write(new String(buf, off, len, connection.outputEncoding()));
    }

    class AeshInputHolder extends InputHolder {

        protected AeshInputHolder(InputHandler handler) {
            super(handler);
        }

        @Override
        protected void setPromptMessage(String prompt) {
            AeshConsole.this.setPromptMessage(prompt);
        }

        @Override
        protected void setStatusMessage(String status) {
            AeshConsole.this.setStatusMessage(status);

        }
    }
}
