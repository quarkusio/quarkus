package org.jboss.resteasy.reactive.client;

/**
 * Represents the entire SSE response from the server
 */
public interface SseEvent<T> {

    /**
     * Get event identifier.
     * <p>
     * Contains value of SSE {@code "id"} field. This field is optional. Method may return {@code null}, if the event
     * identifier is not specified.
     *
     * @return event id.
     */
    String id();

    /**
     * Get event name.
     * <p>
     * Contains value of SSE {@code "event"} field. This field is optional. Method may return {@code null}, if the event
     * name is not specified.
     *
     * @return event name, or {@code null} if not set.
     */
    String name();

    /**
     * Get a comment string that accompanies the event.
     * <p>
     * Contains value of the comment associated with SSE event. This field is optional. Method may return {@code null}, if
     * the event comment is not specified.
     *
     * @return comment associated with the event.
     */
    String comment();

    /**
     * Get event data.
     */
    T data();
}
