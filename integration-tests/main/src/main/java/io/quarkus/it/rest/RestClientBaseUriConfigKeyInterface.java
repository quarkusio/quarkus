package io.quarkus.it.rest;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Used to test default @{@link Dependent} scope defined on interface
 */
@RegisterRestClient(baseUri = "htpp://rest-client-test-dummy-url", configKey = "restClientBaseUriConfigKey")
@Path("/test")
public interface RestClientBaseUriConfigKeyInterface {

    @GET
    String get();

}
