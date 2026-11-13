package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Uses an https URL while native SSL support is disabled.
 */
@Path("/")
@RegisterRestClient
public interface SslRestInterface {

    @GET
    String get();
}
