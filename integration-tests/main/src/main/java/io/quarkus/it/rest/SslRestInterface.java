package io.quarkus.it.rest;

import jakarta.ws.rs.GET;

/**
 * Disabled by default as it establishes external connections.
 * <p>
 * Uncomment when you want to test SSL support.
 */
//@Path("/")
//@RegisterRestClient
public interface SslRestInterface {

    @GET
    String get();
}
