package io.quarkus.runtime.logging;

import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AsyncConfig {

    /**
     * Indicates whether to log asynchronously
     */
    @ConfigItem(name = ConfigItem.PARENT)
    boolean enable;
    /**
     * The queue length to use before flushing writing
     */
    @ConfigItem(defaultValue = "512")
    int queueLength;

    /**
     * Determine whether to block the publisher (rather than drop the message) when the queue is full
     */
    @ConfigItem(defaultValue = "block")
    OverflowAction overflow;
}
