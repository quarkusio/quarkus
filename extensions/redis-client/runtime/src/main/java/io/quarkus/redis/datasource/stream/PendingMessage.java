package io.quarkus.redis.datasource.stream;

import java.time.Duration;

/**
 * Represents the result of an expended xpending command. In the extended form we no longer see the summary information,
 * instead there is detailed information for each message in the pending entries list. For each message four attributes
 * are returned:
 * <ul>
 * <li>The ID of the message.</li>
 * <li>The name of the consumer that fetched the message and has still to acknowledge it. We call it the current owner
 * of the message.</li>
 * <li>The number of milliseconds that elapsed since the last time this message was delivered to this consumer.</li>
 * <li>The number of times this message was delivered.</li>
 * </ul>
 * <p>
 * While this structure is mutable, it is highly recommended to use it as a read-only structure.
 */
public class PendingMessage {
    private final String messageId;
    private final String consumer;
    private final Duration durationSinceLastDelivery;

    private final int deliveryCount;

    public PendingMessage(String messageId, String consumer, Duration durationSinceLastDelivery, int deliveryCount) {
        this.messageId = messageId;
        this.consumer = consumer;
        this.durationSinceLastDelivery = durationSinceLastDelivery;
        this.deliveryCount = deliveryCount;
    }

    /**
     * Gets the message id.
     *
     * @return the message id;
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Gets the consumer name.
     *
     * @return the consumer name
     */
    public String getConsumer() {
        return consumer;
    }

    /**
     * Gets the duration since the last delivery attempt.
     *
     * @return the duration
     */
    public Duration getDurationSinceLastDelivery() {
        return durationSinceLastDelivery;
    }

    /**
     * Gets the number of delivery attempts.
     *
     * @return the number of attempts
     */
    public int getDeliveryCount() {
        return deliveryCount;
    }
}
