package org.jboss.resteasy.reactive.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

/**
 * Used when not all SSE events streamed from the server should be included in the event stream returned by the client.
 * <p>
 * IMPORTANT: implementations MUST contain a no-args constructor
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SseEventFilter {

    /**
     * Predicate which decides whether an event should be included in the event stream returned by the client.
     */
    Class<? extends Predicate<SseEvent<String>>> value();
}
