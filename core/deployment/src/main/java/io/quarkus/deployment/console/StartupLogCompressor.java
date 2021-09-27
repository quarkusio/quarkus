package io.quarkus.deployment.console;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import io.quarkus.deployment.dev.testing.MessageFormat;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.console.StatusLine;

/**
 * special filter that can be used to compress log messages to a status line
 * <p>
 * This is useful for Dev Services to show progress without cluttering up the logs
 */
public class StartupLogCompressor implements Closeable, BiPredicate<String, Boolean> {

    final Thread thread;
    final String name;
    final StatusLine sl;
    final List<String> toDump = new ArrayList<>();
    final AtomicInteger COUNTER = new AtomicInteger();

    public StartupLogCompressor(String name,
            @SuppressWarnings("unused") Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            @SuppressWarnings("unused") LoggingSetupBuildItem loggingSetupBuildItem) {
        if (QuarkusConsole.INSTANCE.isAnsiSupported()) {
            QuarkusConsole.installRedirects();
            this.name = name;
            this.thread = Thread.currentThread();
            QuarkusConsole.addOutputFilter(this);
            sl = QuarkusConsole.INSTANCE.registerStatusLine(1000 + COUNTER.incrementAndGet());
            sl.setMessage(MessageFormat.BLUE + name + MessageFormat.RESET);
        } else {
            thread = null;
            this.name = null;
            sl = null;
        }
    }

    @Override
    public void close() {
        if (thread == null) {
            return;
        }
        QuarkusConsole.removeOutputFilter(this);
        sl.close();
    }

    public void closeAndDumpCaptured() {
        if (thread == null) {
            return;
        }
        QuarkusConsole.removeOutputFilter(this);
        sl.close();
        for (var i : toDump) {
            QuarkusConsole.INSTANCE.write(true, i);
        }
    }

    @Override
    public boolean test(String s, Boolean errorStream) {
        if (thread == null) {
            //not installed
            return true;
        }
        if (Thread.currentThread() == thread) {
            toDump.add(s);
            sl.setMessage(MessageFormat.BLUE + name + MessageFormat.RESET + " " + s.replace("\n", ""));
            return false;
        }
        return true;
    }

}
