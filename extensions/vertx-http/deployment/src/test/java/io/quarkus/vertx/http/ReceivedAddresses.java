package io.quarkus.vertx.http;

import java.util.Optional;

import io.quarkus.runtime.Address;

public record ReceivedAddresses(Optional<Address> main, Optional<Address> management) {
}
