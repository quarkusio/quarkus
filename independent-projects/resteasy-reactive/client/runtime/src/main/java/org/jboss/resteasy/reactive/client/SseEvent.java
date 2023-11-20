package org.jboss.resteasy.reactive.client;

/**
 * Represents the entire SSE response from the server
 */
public interface SseEvent<T> {

    String id();

    String name();

    String comment();

    T data();
}
