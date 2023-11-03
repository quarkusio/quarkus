package io.quarkus.deployment.logging;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.logging.LogCleanupFilterElement;

/**
 * Declare that a log filter should be applied to the specified <tt>loggerName</tt>,
 * provided the message starts with <tt>messageStart</tt>.
 *
 * @author Stéphane Épardaud
 */
public final class LogCleanupFilterBuildItem extends MultiBuildItem {

    private LogCleanupFilterElement filterElement;

    public LogCleanupFilterBuildItem(String loggerName, String... messageStarts) {
        this(loggerName, Arrays.asList(messageStarts));
    }

    public LogCleanupFilterBuildItem(String loggerName, Level targetLevel, String... messageStarts) {
        this(loggerName, targetLevel, Arrays.asList(messageStarts));
    }

    public LogCleanupFilterBuildItem(String loggerName, List<String> messageStarts) {
        if (messageStarts.isEmpty()) {
            throw new IllegalArgumentException("messageStarts cannot be empty");
        }
        this.filterElement = new LogCleanupFilterElement(loggerName, messageStarts);
    }

    public LogCleanupFilterBuildItem(String loggerName, Level targetLevel, List<String> messageStarts) {
        if (messageStarts.isEmpty()) {
            throw new IllegalArgumentException("messageStarts cannot be empty");
        }
        this.filterElement = new LogCleanupFilterElement(loggerName, targetLevel, messageStarts);
    }

    public LogCleanupFilterElement getFilterElement() {
        return filterElement;
    }
}
