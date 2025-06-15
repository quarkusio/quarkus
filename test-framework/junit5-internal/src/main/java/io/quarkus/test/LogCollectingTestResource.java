package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logmanager.formatters.PatternFormatter;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class LogCollectingTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String LOGGER = "logger";
    public static final String LEVEL = "level";
    public static final String EXCLUDE = "exclude";
    public static final String INCLUDE = "include";

    private static final Formatter LOG_FORMATTER = new PatternFormatter("%s");

    public static String format(LogRecord record) {
        return LOG_FORMATTER.format(record);
    }

    public static LogCollectingTestResource current() {
        if (current == null) {
            throw new IllegalStateException(
                    LogCollectingTestResource.class.getName() + " must be registered with @QuarkusTestResource");
        }
        return current;
    }

    private static final Logger rootLogger;
    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        Logger logger = LogManager.getLogManager().getLogger("");
        if (!(logger instanceof org.jboss.logmanager.Logger)) {
            throw new IllegalStateException(
                    "LogCollectingTestResource must be used with the the JBoss LogManager. See https://quarkus.io/guides/logging#how-to-configure-logging-for-quarkustest for an example of how to configure it in Maven.");
        }
        rootLogger = logger;
    }

    private static LogCollectingTestResource current;

    private Logger logger;
    private volatile boolean logHandlerDidHandleLogs = false;
    private InMemoryLogHandler inMemoryLogHandler;

    public List<LogRecord> getRecords() {
        // Just to avoid a situation where the log handler stops working
        // and the test passes regardless of what's actually logged...
        assertTrue(logHandlerDidHandleLogs,
                "The log handler didn't handle even one log record (ignoring filters); something is wrong with the setup."
                        + " Note this test resource will only work correctly with @QuarkusTest:"
                        + " QuarkusDevModeTest, QuarkusProdModeTest and QuarkusUnitTest expose their own log assertion features,"
                        + " and @QuarkusIntegrationTest simply cannot support this feature in its current form.");
        return inMemoryLogHandler.getRecords();
    }

    public void clear() {
        inMemoryLogHandler.clearRecords();
    }

    @Override
    public void init(Map<String, String> initArgs) {
        String loggerName = initArgs.get(LOGGER);
        if (loggerName == null) {
            logger = rootLogger;
        } else {
            logger = LogManager.getLogManager().getLogger(loggerName);
        }
        List<Pattern> excludes = fromCommaSeparatedRegexStrings(initArgs.get(EXCLUDE));
        List<Pattern> includes = fromCommaSeparatedRegexStrings(initArgs.get(INCLUDE));
        String levelAsString = initArgs.get(LEVEL);
        Level level = levelAsString != null ? Level.parse(levelAsString) : Level.INFO;

        inMemoryLogHandler = new InMemoryLogHandler(record -> {
            logHandlerDidHandleLogs = true;
            if (record.getLevel().intValue() < level.intValue()) {
                return false;
            }
            String recordLoggerName = record.getLoggerName();
            for (Pattern exclude : excludes) {
                if (exclude.matcher(recordLoggerName).matches()) {
                    return false;
                }
            }
            if (includes.isEmpty()) {
                return true;
            }
            for (Pattern include : includes) {
                if (include.matcher(recordLoggerName).matches()) {
                    return true;
                }
            }
            return false;
        });
    }

    private List<Pattern> fromCommaSeparatedRegexStrings(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(",")).map(Pattern::compile).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> start() {
        inMemoryLogHandler.getRecords().clear();
        logHandlerDidHandleLogs = false;
        logger.addHandler(inMemoryLogHandler);
        if (current != null) {
            throw new IllegalStateException(
                    LogCollectingTestResource.class.getName() + " used concurrently from multiple tests?");
        }
        current = this;
        return Map.of();
    }

    @Override
    public void stop() {
        current = null;
        logger.removeHandler(inMemoryLogHandler);
    }
}
