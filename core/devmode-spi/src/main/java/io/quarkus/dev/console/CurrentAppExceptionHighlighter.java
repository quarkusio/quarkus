package io.quarkus.dev.console;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.LogRecord;

/**
 * A bit of a hack, but currently there is no other way to do this.
 *
 * This allows for exceptions to be formatted to be bold/underlined in the console if they
 * are part of the user application, which makes it much easier to see the interesting
 * parts of large stack traces
 */
public class CurrentAppExceptionHighlighter {

    public static volatile BiConsumer<LogRecord, Consumer<LogRecord>> THROWABLE_FORMATTER;

}
