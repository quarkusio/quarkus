package io.quarkus.test.integration.mtls;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "test-api")
@Path("test")
@Produces(APPLICATION_JSON)
public interface TestAPI {

    @GET
    @Path("names")
    Uni<List<String>> getNames();

}
