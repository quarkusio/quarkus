package io.quarkus.deployment.logging;

import org.jboss.builder.item.MultiBuildItem;

import io.quarkus.runtime.logging.LogCleanupFilterElement;

/**
 * Declare that a log filter should be applied to the specified <tt>loggerName</tt>,
 * provided the message starts with <tt>messageStart</tt>.
 *
 * @author Stéphane Épardaud
 */
public final class LogCleanupFilterBuildItem extends MultiBuildItem {

    private LogCleanupFilterElement filterElement;

    public LogCleanupFilterBuildItem(String loggerName, String messageStart) {
        this.filterElement = new LogCleanupFilterElement(loggerName, messageStart);
    }

    public LogCleanupFilterElement getFilterElement() {
        return filterElement;
    }
}
