package io.quarkus.test.common;

import java.util.Optional;

/**
 * Holds the result of a listening-data capture: the main HTTP/HTTPS server and the optional management server.
 */
public record ListeningResults(Optional<ListeningResult> server,
        Optional<ListeningResult> management) {

    public static final ListeningResults EMPTY = new ListeningResults(Optional.empty(), Optional.empty());
}
