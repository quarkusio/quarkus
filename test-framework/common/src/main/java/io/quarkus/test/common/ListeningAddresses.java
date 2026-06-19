package io.quarkus.test.common;

import java.util.Optional;

/**
 * Holds the result of a listening-data capture: the main HTTP/HTTPS address and the optional management address.
 */
public record ListeningAddresses(Optional<ListeningAddress> address, Optional<ListeningAddress> managementAddress) {

    public static final ListeningAddresses EMPTY = new ListeningAddresses(Optional.empty(), Optional.empty());
}
