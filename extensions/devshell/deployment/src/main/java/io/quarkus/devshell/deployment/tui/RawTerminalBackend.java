package io.quarkus.devshell.deployment.tui;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

/**
 * Terminal backend using raw System.in/System.out with stty.
 * Used for standalone Dev Shell sessions connecting from another terminal.
 */
public class RawTerminalBackend implements TerminalBackend {

    private static final Logger log = Logger.getLogger(RawTerminalBackend.class);

    private final InputStream in;
    private final PrintStream out;
    private volatile Consumer<int[]> inputHandler;
    private volatile boolean running;
    private Thread readerThread;
    private String savedStty;

    public RawTerminalBackend() {
        this(System.in, System.out);
    }

    public RawTerminalBackend(InputStream in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    public void start() {
        savedStty = stty("-g");
        stty("raw -echo -icanon min 1");

        running = true;
        readerThread = new Thread(this::readLoop, "Dev Shell Input Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        byte[] buf = new byte[64];
        while (running) {
            try {
                int available = in.available();
                if (available <= 0) {
                    Thread.sleep(10);
                    continue;
                }
                int n = in.read(buf, 0, Math.min(available, buf.length));
                if (n <= 0) {
                    break;
                }
                int[] keys = new int[n];
                for (int i = 0; i < n; i++) {
                    keys[i] = buf[i] & 0xFF;
                }
                Consumer<int[]> handler = inputHandler;
                if (handler != null) {
                    handler.accept(keys);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                if (running) {
                    log.debug("Input read error", e);
                }
                break;
            }
        }
    }

    @Override
    public void write(String output) {
        out.print(output);
        out.flush();
    }

    @Override
    public void setInputHandler(Consumer<int[]> handler) {
        this.inputHandler = handler;
    }

    @Override
    public void setResizeHandler(Consumer<int[]> handler) {
        // SIGWINCH not easily captured in pure Java; size polled on render
    }

    @Override
    public int getWidth() {
        return queryTerminalSize()[0];
    }

    @Override
    public int getHeight() {
        return queryTerminalSize()[1];
    }

    @Override
    public void enterAlternateScreen() {
        write("\033[?1049h");
    }

    @Override
    public void exitAlternateScreen() {
        write("\033[?1049l");
    }

    @Override
    public void hideCursor() {
        write("\033[?25l");
    }

    @Override
    public void showCursor() {
        write("\033[?25h");
    }

    @Override
    public void close() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt();
        }
        showCursor();
        exitAlternateScreen();
        write("\033[0m");
        if (savedStty != null) {
            stty(savedStty);
        }
    }

    private int[] queryTerminalSize() {
        try {
            String size = stty("size");
            if (size != null && !size.isEmpty()) {
                String[] parts = size.trim().split("\\s+");
                if (parts.length == 2) {
                    return new int[] { Integer.parseInt(parts[1]), Integer.parseInt(parts[0]) };
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return new int[] { 80, 24 };
    }

    private static String stty(String args) {
        try {
            String[] cmd = { "/bin/sh", "-c", "stty " + args + " < /dev/tty" };
            Process p = Runtime.getRuntime().exec(cmd);
            String result = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return result;
        } catch (Exception e) {
            return "";
        }
    }
}
