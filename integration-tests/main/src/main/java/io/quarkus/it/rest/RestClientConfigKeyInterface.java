package io.quarkus.it.rest;

import javax.enterprise.context.Dependent;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
