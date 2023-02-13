package io.quarkus.it.rest;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Used to test default @{@link Dependent} scope defined on interface
 */
@RegisterRestClient(configKey = "restClientConfigKey")
@Path("/test")
public interface RestClientConfigKeyInterface {

    @GET
    String get();

}
