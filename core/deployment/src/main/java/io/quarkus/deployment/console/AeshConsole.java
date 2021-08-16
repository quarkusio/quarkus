package io.quarkus.deployment.console;

import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Size;

import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.console.StatusLine;

public class AeshConsole extends QuarkusConsole {

    private final Connection connection;
    private Size size;
    private Attributes attributes;

    private String[] messages = new String[0];
    private int totalStatusLines = 0;
    private int lastWriteCursorX;
    private String lastColorCode; //foreground color code, or reset
    private volatile boolean doingReadline;
    /**
     * if the status area has gotten big then small again
     * this tracks how many lines of blank space we have
     * so we start writing in the correct place.
     */
    private int bottomBlankSpace = 0;
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

    static final Pattern ESCAPE = Pattern.compile("\u001b\\[(\\d\\d?)[\\d;]*m");

    static final TreeMap<Integer, StatusLineImpl> statusMap = new TreeMap<>();
    private final ReadWriteLock positionLock = new ReentrantReadWriteLock();
    private volatile boolean closed;

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
        }, "Console Shutdown Hook"));
    }

    private void updatePromptOnChange(StringBuilder buffer, int newLines) {
        if (newLines > totalStatusLines) {
            StringBuilder nb = new StringBuilder();
            for (int i = 0; i < newLines - totalStatusLines; ++i) {
                if (bottomBlankSpace > 0) {
                    bottomBlankSpace--;
                } else {
                    nb.append("\n");
                }
            }
            writeQueue.add(nb.toString());
            deadlockSafeWrite();
        } else if (newLines < totalStatusLines) {
            bottomBlankSpace = bottomBlankSpace + (totalStatusLines - newLines);
        }
        this.totalStatusLines = newLines;
        printStatusAndPrompt(buffer);
        writeQueue.add(buffer.toString());
    }

    @Override
    public StatusLine registerStatusLine(int priority) {
        try {
            positionLock.writeLock().lock();
            while (statusMap.containsKey(priority)) {
                //this kinda sucks, but it means that if multiple extensions try and
                //use this and happen to pick the same priority things don't blow up
                priority++;
            }
            StatusLineImpl value = new StatusLineImpl(priority);
            statusMap.put(priority, value);
            rebalance();
            return value;
        } finally {
            positionLock.writeLock().unlock();
        }
    }

    @Override
    public void setPromptMessage(String promptMessage) {
        setMessage(0, promptMessage);
    }

    private AeshConsole setMessage(int position, String message) {
        synchronized (this) {
            if (messages.length <= position) {
                String[] old = messages;
                messages = new String[position + 1];
                System.arraycopy(old, 0, this.messages, 0, old.length);
            }
            messages[position] = message;
            int newLines = countTotalStatusLines();
            StringBuilder buffer = new StringBuilder();
            clearStatusMessages(buffer);
            updatePromptOnChange(buffer, newLines);
        }
        deadlockSafeWrite();
        return this;
    }

    private int countTotalStatusLines() {
        int total = 0;
        for (String i : messages) {
            if (i != null) {
                total++;
                total += countLines(i);
            }
        }
        return total == 0 ? total : total + 1;
    }

    private void end(Connection conn) {
        conn.setAttributes(attributes);
        StringBuilder sb = new StringBuilder();
        sb.append("\u001B[0m\n");
        writeQueue.add(sb.toString());
        deadlockSafeWrite();
        closed = true;
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
                var handler = inputHandler;
                if (handler != null) {
                    handler.accept(keys);
                }
                if (doingReadline) {
                    for (var k : keys) {
                        if (k == '\n') {
                            doingReadline = false;
                            connection.enterRawMode();
                        }
                    }
                }
            });

            conn.setCloseHandler(close -> end(conn));
            conn.setSizeHandler(size -> setup(conn));

            attributes = conn.enterRawMode();

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
        if (totalStatusLines == 0 || closed) {
            return;
        } else if (totalStatusLines < size.getHeight()) {
            //if the console is tiny we don't do this
            clearStatusMessages(buffer);
            gotoLine(buffer, size.getHeight() - totalStatusLines);
        } else {
            bottomBlankSpace = 0;
        }
        buffer.append("\n--\n");
        for (int i = messages.length - 1; i >= 0; --i) {
            String msg = messages[i];
            if (msg != null) {
                buffer.append(msg);
                if (i > 0) {
                    //if there is any more messages to print we add a newline
                    for (int j = 0; j < i; ++j) {
                        if (messages[j] != null) {
                            buffer.append("\n");
                            break;
                        }
                    }
                }
            }
        }
    }

    private void clearStatusMessages(StringBuilder buffer) {
        gotoLine(buffer, size.getHeight() - totalStatusLines + 1);
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
        if (closed) {
            // we need to guard against Aesh logging since the connection.write call, in certain error conditions,
            // can lead to a logger message write, which can trigger an infinite loop
            IN_WRITE.set(true);
            try {
                connection.write(s);
                return;
            } finally {
                IN_WRITE.set(false);
            }
        }
        if (lastColorCode != null) {
            s = lastColorCode + s;
        }
        Matcher m = ESCAPE.matcher(s);
        while (m.find()) {
            int val = Integer.parseInt(m.group(1));
            if (val == 0 || //reset
                    (val >= 30 && val <= 39) || //foreground colors
                    (val >= 90 && val <= 97)) { //bright foreground colors
                lastColorCode = m.group(0);
            }
        }

        StringBuilder buffer = new StringBuilder();
        synchronized (this) {
            if (outputFilter != null) {
                if (!outputFilter.test(s)) {
                    return;
                }
            }
            if (totalStatusLines == 0) {
                bottomBlankSpace = 0; //just to be safe, will only happen if status is added then removed, which is not really likely
                writeQueue.add(s);
            } else {
                clearStatusMessages(buffer);
                int cursorPos = lastWriteCursorX;
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
                int usedBlankSpace = 0;
                gotoLine(buffer, size.getHeight());
                if (cursorPos > 1 && lines == 0) {
                    gotoLine(buffer, size.getHeight() - bottomBlankSpace);
                    buffer.append(s);
                    lastWriteCursorX = newCursorPos;
                    //partial line, just write it
                    writeQueue.add(buffer.toString());
                } else {
                    if (lines == 0) {
                        lines++;
                    }
                    int originalBlank = bottomBlankSpace;
                    if (bottomBlankSpace > 0) {
                        usedBlankSpace = Math.min(bottomBlankSpace, lines);
                        bottomBlankSpace -= usedBlankSpace;
                    }
                    //move the existing content up by the number of lines
                    int appendLines = Math.max(Math.min(cursorPos > 1 ? lines - 1 : lines, totalStatusLines), 1);
                    appendLines -= usedBlankSpace;
                    clearStatusMessages(buffer);
                    buffer.append("\033[").append(size.getHeight() - totalStatusLines - originalBlank).append(";")
                            .append(lastWriteCursorX)
                            .append("H");
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
        }
        deadlockSafeWrite();
    }

    public void write(byte[] buf, int off, int len) {
        write(new String(buf, off, len, connection.outputEncoding()));
    }

    @Override
    public void doReadLine() {
        setPromptMessage("");
        connection.setAttributes(attributes);
        doingReadline = true;

    }

    void rebalance() {
        int count = 1;
        for (var val : statusMap.values()) {
            val.position = count;
            setMessage(count++, val.message);
        }
    }

    class StatusLineImpl implements StatusLine {

        final int priority;
        int position;
        String message;

        StatusLineImpl(int priority) {
            this.priority = priority;
        }

        @Override
        public void setMessage(String message) {
            this.message = message;
            try {
                positionLock.readLock().lock();
                AeshConsole.this.setMessage(position, message);
            } finally {
                positionLock.readLock().unlock();
            }
        }

        @Override
        public void close() {
            positionLock.writeLock().lock();
            try {
                AeshConsole.this.setMessage(position, null);
                statusMap.remove(priority);
                rebalance();
            } finally {
                positionLock.writeLock().unlock();
            }
        }
    }
}
