package io.quarkus.funqy.lambda.event;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.quarkus.funqy.lambda.config.FunqyAmazonConfig;

/**
 * This interface described how events should be handled
 *
 * @param <E> type of the event
 * @param <M> type of the message
 * @param <R> type of the response
 */
public interface EventHandler<E, M, R> {

    /**
     * Provides all messages from the event. Specially for events with multiple messages from a batch.
     *
     * @param event event to provide messages from
     * @param amazonConfig config
     * @return a stream of messages
     */
    Stream<M> streamEvent(E event, FunqyAmazonConfig amazonConfig);

    /**
     * Get the identifier of a message.
     *
     * @param message message to extract the identifier from
     * @param amazonConfig config
     * @return the identifier
     */
    String getIdentifier(M message, FunqyAmazonConfig amazonConfig);

    /**
     * Get the body of a message as an {@link InputStream}
     *
     * @param message message to extract the body from
     * @param amazonConfig config
     * @return the body input stream
     */
    Supplier<InputStream> getBody(M message, FunqyAmazonConfig amazonConfig);

    /**
     * Create the response based on the collected failures.
     *
     * @param failures a list of message identifier, which failed
     * @param amazonConfig config
     * @return the created response
     */
    R createResponse(List<String> failures, FunqyAmazonConfig amazonConfig);

    /**
     * The class of the message
     *
     * @return the class of the message
     */
    Class<M> getMessageClass();
}
