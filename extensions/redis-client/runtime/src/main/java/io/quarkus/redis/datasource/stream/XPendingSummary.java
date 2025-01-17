package io.quarkus.redis.datasource.stream;

import java.util.Map;

/**
 * The result of the xpending command when using the summary form.
 * <p>
 * When using the summary form of xpending, the command outputs the total number of pending messages for this consumer
 * group, followed by the smallest and greatest ID among the pending messages, and then list every consumer in the
 * consumer group with at least one pending message, and the number of pending messages it has.
 */
public class XPendingSummary {

    private final long pendingCount;

    private final String lowestId;
    private final String highestId;

    private final Map<String, Long> consumers;

    public XPendingSummary(long pendingCount, String lowestId, String highestId, Map<String, Long> consumers) {
        this.pendingCount = pendingCount;
        this.lowestId = lowestId;
        this.highestId = highestId;
        this.consumers = consumers;
    }

    /**
     * Gets the number of message waiting for acknowledgement
     *
     * @return the number of message not yet acknowledged
     */
    public long getPendingCount() {
        return pendingCount;
    }

    /**
     * Gets the lowest message id that was not yet acknowledged.
     *
     * @return the lowest message id; may be {@code null}
     */
    public String getLowestId() {
        return lowestId;
    }

    /**
     * Gets the highest message id that was not yet acknowledged.
     *
     * @return the highest message id; may be {@code null}
     */
    public String getHighestId() {
        return highestId;
    }

    /**
     * Get the list of every consumer in the consumer group with at least one pending message,
     * and the number of pending messages it has.
     *
     * @return the map composed of consumer -> number of message; may be empty
     */
    public Map<String, Long> getConsumers() {
        return consumers;
    }
}
