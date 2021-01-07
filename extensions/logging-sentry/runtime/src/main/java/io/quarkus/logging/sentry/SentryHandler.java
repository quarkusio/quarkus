package io.quarkus.logging.sentry;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.*;
import java.util.stream.Collectors;

import org.slf4j.MDC;

import io.sentry.DateUtils;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.util.CollectionUtils;

/**
 * Logging handler in charge of sending the java.util.logging records to a Sentry server.
 */
public class SentryHandler extends Handler {
    /**
     * Name of the extra property containing the Thread id.
     */
    public static final String THREAD_ID = "thread_id";

    /**
     * If true, <code>String.format()</code> is used to render parameterized log
     * messages instead of <code>MessageFormat.format()</code>; Defaults to
     * false.
     */
    protected boolean printfStyle;

    private final SentryOptions options;

    /**
     * Creates an instance of SentryHandler.
     */
    public SentryHandler(SentryOptions options) {
        this.options = Objects.requireNonNull(options, "options is required");
        retrieveProperties();
        this.setFilter(new DropSentryFilter());
    }

    /**
     * Transforms a {@link Level} into an {@link SentryLevel}.
     *
     * @param level original level as defined in JUL.
     * @return log level used within sentry.
     */
    protected static SentryLevel getLevel(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return SentryLevel.ERROR;
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            return SentryLevel.WARNING;
        } else if (level.intValue() >= Level.INFO.intValue()) {
            return SentryLevel.INFO;
        } else if (level.intValue() >= Level.ALL.intValue()) {
            return SentryLevel.DEBUG;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the properties of the logger.
     */
    protected void retrieveProperties() {
        LogManager manager = LogManager.getLogManager();
        String className = SentryHandler.class.getName();
        setPrintfStyle(Boolean.parseBoolean(manager.getProperty(className + ".printfStyle")));
        setLevel(parseLevelOrDefault(manager.getProperty(className + ".level")));
    }

    private Level parseLevelOrDefault(String levelName) {
        try {
            return Level.parse(levelName.trim());
        } catch (RuntimeException e) {
            return Level.WARNING;
        }
    }

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            Sentry.captureEvent(createEvent(record));
        }
    }

    /**
     * Builds an EventBuilder based on the log record.
     *
     * @param record Log generated.
     * @return EventBuilder containing details provided by the logging system.
     */
    protected SentryEvent createEvent(LogRecord record) {
        final SentryEvent event = new SentryEvent(DateUtils.getDateTime(new Date(record.getMillis())));
        final String localizedMessage = getLocalizedMessage(record);

        final Message message = new Message();
        message.setMessage(localizedMessage);
        message.setFormatted(getFormattedMessage(record));
        message.setParams(toParams(record.getParameters()));
        event.setMessage(message);
        event.setLogger(record.getLoggerName());
        event.setLevel(getLevel(record.getLevel()));
        event.setThrowable(record.getThrown());
        event.setExtra(THREAD_ID, record.getThreadID());

        final Map<String, String> mdcProperties = CollectionUtils.shallowCopy(MDC.getMDCAdapter().getCopyOfContextMap());
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            event.getContexts().put("MDC", mdcProperties);
        }
        return event;
    }

    private String getFormattedMessage(LogRecord record) {
        final String localizedMessage = getLocalizedMessage(record);
        if (record.getParameters() == null) {
            return localizedMessage;
        }
        try {
            return formatMessage(localizedMessage, record.getParameters());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String getLocalizedMessage(LogRecord record) {
        String message = record.getMessage();
        if (record.getResourceBundle() != null && record.getResourceBundle().containsKey(message)) {
            return record.getResourceBundle().getString(message);
        }
        return message;

    }

    private List<String> toParams(Object[] arguments) {
        if (arguments != null) {
            return Arrays.stream(arguments)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns formatted Event message when provided the message template and
     * parameters.
     *
     * @param message Message template body.
     * @param parameters Array of parameters for the message.
     * @return Formatted message.
     */
    protected String formatMessage(String message, Object[] parameters) {
        String formatted;
        if (printfStyle) {
            formatted = String.format(message, parameters);
        } else {
            formatted = MessageFormat.format(message, parameters);
        }
        return formatted;
    }

    @Override
    public void flush() {
        Sentry.flush(0);
    }

    @Override
    public void close() throws SecurityException {
        try {
            Sentry.close();
        } catch (RuntimeException e) {
            reportError("An exception occurred while closing the Sentry connection", e, ErrorManager.CLOSE_FAILURE);
        }
    }

    public void setPrintfStyle(boolean printfStyle) {
        this.printfStyle = printfStyle;
    }

    public SentryOptions getOptions() {
        return options;
    }

    private class DropSentryFilter implements Filter {
        @Override
        public boolean isLoggable(LogRecord record) {
            String loggerName = record.getLoggerName();
            return loggerName == null || !loggerName.startsWith("io.sentry");
        }
    }
}
