package io.quarkus.it.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;

import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Disabled by default as it establishes external connections.
 * <p>
 * Uncomment when you want to test SSL support.
 */
//@Path("/ssl")
public class SslClientResource {

    @Inject
    @RestClient
    SslRestInterface sslRestInterface;

    @GET
    public String https() throws Exception {
        return sslRestInterface.get();
    }

}
