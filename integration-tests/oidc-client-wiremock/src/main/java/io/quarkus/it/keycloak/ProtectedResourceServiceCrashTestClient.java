package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.filter.OidcClientFilter;

/**
 * Made for use in concurrent tests that need a reproducible state (without token initially) for race conditions.
 */
@RegisterRestClient(configKey = "crash-test")
@OidcClientFilter("crash-test")
@Path("/")
public interface ProtectedResourceServiceCrashTestClient {

    @GET
    String echoToken();
}
