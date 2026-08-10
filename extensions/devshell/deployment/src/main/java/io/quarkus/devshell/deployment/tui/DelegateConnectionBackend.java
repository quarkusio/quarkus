package io.quarkus.devshell.deployment.tui;

import java.util.function.Consumer;

import org.aesh.terminal.tty.Size;

import io.quarkus.deployment.console.DelegateConnection;

/**
 * Terminal backend that wraps an aesh DelegateConnection.
 * Used when Dev Shell runs in-process (pressing 't' in dev mode).
 */
public class DelegateConnectionBackend implements TerminalBackend {

    private final DelegateConnection connection;

    public DelegateConnectionBackend(DelegateConnection connection) {
        this.connection = connection;
    }

    @Override
    public void write(String output) {
        connection.write(output);
    }

    @Override
    public void setInputHandler(Consumer<int[]> handler) {
        connection.setStdinHandler(handler);
    }

    @Override
    public void setResizeHandler(Consumer<int[]> handler) {
        connection.setSizeHandler(size -> handler.accept(new int[] { size.getWidth(), size.getHeight() }));
    }

    @Override
    public int getWidth() {
        Size size = connection.size();
        return size != null ? size.getWidth() : 80;
    }

    @Override
    public int getHeight() {
        Size size = connection.size();
        return size != null ? size.getHeight() : 24;
    }

    @Override
    public void enterAlternateScreen() {
        // handled by AeshConsole.takeoverTerminal()
    }

    @Override
    public void exitAlternateScreen() {
        // handled by AeshConsole.releaseTerminal()
    }

    @Override
    public void hideCursor() {
        connection.write("\033[?25l");
    }

    @Override
    public void showCursor() {
        connection.write("\033[?25h");
    }

    @Override
    public void close() {
        // lifecycle managed by AeshConsole
    }
}
