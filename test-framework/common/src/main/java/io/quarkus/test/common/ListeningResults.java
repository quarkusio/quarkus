package io.quarkus.test.common;

import java.util.Optional;

/**
 * Holds the result of a listening-data capture: the main HTTP/HTTPS address and the optional management address.
 */
public record ListeningResults(Optional<ListeningResult> address, Optional<ListeningAddress> managementAddress) {

    public static final ListeningResults EMPTY = new ListeningResults(Optional.empty(), Optional.empty());
}
