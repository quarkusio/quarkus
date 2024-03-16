package io.quarkus.resteasy.reactive.server;

import java.io.Closeable;

/**
 * A service that allows users to close any {@link Closeable} that
 * when the request completes.
 * <p>
 * Meant to be used a Resource Method parameter using {@link jakarta.ws.rs.core.Context}
 */
public interface Closer {

    /**
     * Register a new {@link Closeable} that is to be closed when the request completes.
     */
    void add(Closeable c);
}
