package io.quarkus.it.rest;

import javax.enterprise.context.Dependent;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
