package io.quarkus.deployment.console;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.registry.CommandRegistry;
import org.aesh.command.settings.Settings;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.readline.ReadlineConsole;
import org.aesh.readline.alias.AliasManager;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Size;

import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.console.StatusLine;

public class AeshConsole extends QuarkusConsole {

    public static final String ALTERNATE_SCREEN_BUFFER = "\u001b[?1049h\n";
    public static final String EXIT_ALTERNATE_SCREEN = "\u001b[?1049l";
    public static final String ALIAS_FILE = ".quarkus/console-aliases.txt";
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
     * Because Aesh can log deadlocks are possible on Windows if a write fails, unless care
     * is taken.
     */
    private final ConcurrentLinkedQueue<String> writeQueue = new ConcurrentLinkedQueue<>();
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
    private final StatusLine prompt;

    private volatile boolean pauseOutput;
    private volatile boolean firstConsoleRun = true;
    private DelegateConnection delegateConnection;
    private ReadlineConsole aeshConsole;

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
        prompt = registerStatusLine(0);

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
        prompt.setMessage(promptMessage);
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
            if (pauseOutput) {
                //output is paused
                return;
            }
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
                        if (delegateConnection != null) {
                            exitCliMode();
                            return;
                        }
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

                QuarkusConsole.StateChangeInputStream redirectIn = QuarkusConsole.REDIRECT_IN;
                // redirectIn might have not been initialized yet
                if (redirectIn == null) {
                    return;
                }
                //see if the users application wants to read the keystrokes:
                int pos = 0;
                while (pos < keys.length) {
                    if (!redirectIn.acceptInput(keys[pos])) {
                        break;
                    }
                    ++pos;
                }
                if (pos > 0) {
                    if (pos == keys.length) {
                        return;
                    }
                    //the app only consumed some keys
                    //stick the rest in a new array
                    int[] newKeys = new int[keys.length - pos];
                    System.arraycopy(keys, pos, newKeys, 0, newKeys.length);
                    keys = newKeys;
                }
                try {
                    if (delegateConnection != null) {
                        //console mode
                        //just sent the input to the delegate
                        if (keys.length == 1) {
                            for (var k : keys) {
                                if (k == 27) { // escape key
                                    exitCliMode();
                                    return;
                                }
                            }
                        }
                        if (delegateConnection.getStdinHandler() != null) {
                            try {
                                delegateConnection.getStdinHandler().accept(keys);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }

                        }
                        return;
                    }
                    var handler = inputHandler;
                    if (handler != null) {
                        try {
                            handler.accept(keys);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                    if (doingReadline) {
                        for (var k : keys) {
                            if (k == '\n' || k == '\r') {
                                doingReadline = false;
                                connection.enterRawMode();
                            }
                        }
                    } else {
                        for (var k : keys) {
                            if (k == ':') {
                                runAeshCli();
                            }
                        }
                    }
                } catch (Throwable t) {
                    //can't reliably use logging here
                    t.printStackTrace();
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
        return gotoCoords(builder, line, 0);
    }

    private StringBuilder gotoCoords(StringBuilder builder, int line, int col) {
        return builder.append("\033[").append(line).append(";").append(col).append("H");
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

    public void write(boolean errorStream, String s) {
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
        if (!shouldWrite(errorStream, s)) {
            return;
        }
        synchronized (this) {
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
                if (cursorPos > 1 && lines == 0) {
                    gotoCoords(buffer, size.getHeight() - bottomBlankSpace - totalStatusLines - 1, cursorPos + 1);
                    buffer.append(s);
                    lastWriteCursorX = newCursorPos;
                    //partial line, just write it
                    writeQueue.add(buffer.toString());
                } else {
                    gotoLine(buffer, size.getHeight());
                    if (lines == 0) {
                        lines++;
                    }
                    int originalBlank = bottomBlankSpace;
                    if (bottomBlankSpace > 0) {
                        usedBlankSpace = Math.min(bottomBlankSpace, lines);
                        bottomBlankSpace -= usedBlankSpace;
                    }
                    //move the existing content up by the number of lines
                    int appendLines = Math.max(
                            Math.min(cursorPos > 1 ? lines - 1 : lines, totalStatusLines + 1) //+1 for the extra blank line above the status line
                            , 1);
                    appendLines -= usedBlankSpace;
                    clearStatusMessages(buffer);
                    buffer.append("\033[").append(size.getHeight() - totalStatusLines - originalBlank - (cursorPos > 0 ? 1 : 0))
                            .append(";")
                            .append(cursorPos + 1)
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

    public void write(boolean errorStream, byte[] buf, int off, int len) {
        write(errorStream, new String(buf, off, len, connection.outputEncoding()));
    }

    @Override
    public boolean isAnsiSupported() {
        return true;
    }

    @Override
    public void doReadLine() {
        setPromptMessage("");
        connection.setAttributes(attributes);
        doingReadline = true;

    }

    void rebalance() {
        synchronized (this) {
            int count = 1;
            messages = new String[statusMap.size()];
            for (var val : statusMap.values()) {
                val.position = count;
                setMessage(count++, val.message);
            }
        }
    }

    class StatusLineImpl implements StatusLine {

        final int priority;
        int position;
        String message;
        boolean closed;

        StatusLineImpl(int priority) {
            this.priority = priority;
        }

        @Override
        public void setMessage(String message) {
            try {
                positionLock.readLock().lock();
                this.message = message;
                if (!closed) {
                    AeshConsole.this.setMessage(position, message);
                }
            } finally {
                positionLock.readLock().unlock();
            }
        }

        @Override
        public void close() {
            positionLock.writeLock().lock();
            closed = true;
            try {
                AeshConsole.this.setMessage(position, null);
                statusMap.remove(priority);
                rebalance();
            } finally {
                positionLock.writeLock().unlock();
            }
        }
    }

    public void runAeshCli() {
        if (ConsoleCliManager.commands.isEmpty()) {
            System.out.println("No commands registered, please wait for Quarkus to start before using the console");
            return;
        }
        try {
            pauseOutput = true;
            delegateConnection = new DelegateConnection(connection);
            connection.write(ALTERNATE_SCREEN_BUFFER);
            if (firstConsoleRun) {
                connection.write(
                        "You are now in Quarkus Terminal. Your app is still running. Use `help` or tab completion to explore, `quit` or `q` to return to your application.\n");
                firstConsoleRun = false;
            }
            AeshCommandRegistryBuilder<CommandInvocation> commandBuilder = AeshCommandRegistryBuilder.builder();
            ConsoleCliManager.commands.forEach(commandBuilder::command);

            CommandRegistry registry = commandBuilder
                    .create();
            Settings settings = SettingsBuilder
                    .builder()
                    .enableExport(false)
                    .enableAlias(true)
                    .aliasManager(
                            new AliasManager(Paths.get(System.getProperty("user.home")).resolve(ALIAS_FILE).toFile(), true))
                    .connection(delegateConnection)
                    .commandRegistry(registry)
                    .build();
            aeshConsole = new ReadlineConsole(settings);
            aeshConsole.setPrompt("quarkus$ ");
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        aeshConsole.start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, "Quarkus integrated CLI thread");
            t.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<Character, String> singleLetterAliases() {
        try {
            var manager = new AliasManager(Paths.get(System.getProperty("user.home")).resolve(ALIAS_FILE).toFile(), true);
            Map<Character, String> ret = new HashMap<>();
            for (String alias : manager.getAllNames()) {
                if (alias.length() == 1) {
                    ret.put(alias.charAt(0), manager.getAlias(alias).get().getValue());
                }
            }
            return ret;
        } catch (IOException e) {
            return Map.of();
        }
    }

    @Override
    public void runAlias(char alias) {
        try {
            AeshCommandRegistryBuilder<CommandInvocation> commandBuilder = AeshCommandRegistryBuilder.builder();
            ConsoleCliManager.commands.forEach(commandBuilder::command);

            CommandRegistry registry = commandBuilder
                    .create();
            Settings settings = SettingsBuilder
                    .builder()
                    .enableExport(false)
                    .inputStream(new ByteArrayInputStream(new byte[] { (byte) alias, '\n' }))
                    .enableAlias(true)
                    .aliasManager(
                            new AliasManager(Paths.get(System.getProperty("user.home")).resolve(ALIAS_FILE).toFile(), true))
                    .connection(delegateConnection)
                    .commandRegistry(registry)
                    .build();
            aeshConsole = new ReadlineConsole(settings);
            aeshConsole.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void exitCliMode() {
        if (aeshConsole == null || delegateConnection == null) {
            return;
        }
        aeshConsole.stop();
        aeshConsole = null;
        delegateConnection.close();
        delegateConnection = null;
        connection.enterRawMode();
        //exit alternate screen mode
        connection.write(EXIT_ALTERNATE_SCREEN);
        pauseOutput = false;
        write(false, "");
        deadlockSafeWrite();
    }
}
