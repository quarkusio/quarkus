package io.quarkus.deployment.dev.console;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.ANSI;

import io.quarkus.dev.console.InputHandler;
import io.quarkus.dev.console.QuarkusConsole;

public class AeshConsole extends QuarkusConsole {

    private final Connection connection;
    private Size size;
    private Attributes attributes;

    private String statusMessage;
    private String promptMessage;
    private int totalStatusLines = 0;
    private int lastWriteCursorX;

    public AeshConsole(Connection connection) {
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

    private synchronized AeshConsole setStatusMessage(String statusMessage) {
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
        connection.write(buffer.toString());
        return this;
    }

    public AeshInputHolder createHolder(InputHandler inputHandler) {
        return new AeshInputHolder(inputHandler);
    }

    private synchronized AeshConsole setPromptMessage(String promptMessage) {
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
        connection.write(buffer.toString());
        return this;
    }

    private synchronized void end(Connection conn) {
        conn.write(ANSI.MAIN_BUFFER);
        conn.write(ANSI.CURSOR_SHOW);
        conn.setAttributes(attributes);
        conn.write("\033[c");
    }

    private void setup(Connection conn) {
        size = conn.size();
        // Ctrl-C ends the game
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

        conn.setCloseHandler(close -> end(conn));
        conn.setSizeHandler(size -> setup(conn));

        //switch to alternate buffer
        //conn.write(ANSI.ALTERNATE_BUFFER);
        //conn.write(ANSI.CURSOR_HIDE);

        attributes = conn.enterRawMode();

        StringBuilder sb = new StringBuilder();
        printStatusAndPrompt(sb);
        conn.write(sb.toString());
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

    public synchronized void write(String s) {
        if (outputFilter != null) {
            if (!outputFilter.test(s)) {
                return;
            }
        }
        StringBuilder buffer = new StringBuilder();
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
        int appendLines = cursorPos > 1 ? lines - 1 : lines;
        for (int i = 0; i < appendLines; ++i) {
            buffer.append("\n");
        }
        buffer.append("\033[").append(size.getHeight() - totalStatusLines - lines).append(";").append(0).append("H");
        buffer.append(s);
        lastWriteCursorX = newCursorPos;
        printStatusAndPrompt(buffer);
        connection.write(buffer.toString());

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
