package io.quarkus.devshell.deployment.tui;

import java.util.function.Consumer;

/**
 * Abstraction over the terminal I/O channel.
 * Allows the TUI to run either in-process (via AeshConsole's DelegateConnection)
 * or standalone from a separate terminal (via JLine).
 */
public interface TerminalBackend extends AutoCloseable {

    void write(String output);

    void setInputHandler(Consumer<int[]> handler);

    void setResizeHandler(Consumer<int[]> handler);

    int getWidth();

    int getHeight();

    void enterAlternateScreen();

    void exitAlternateScreen();

    void hideCursor();

    void showCursor();

    @Override
    void close();
}
