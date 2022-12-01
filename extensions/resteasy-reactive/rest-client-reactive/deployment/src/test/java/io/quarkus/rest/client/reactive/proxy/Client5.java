package io.quarkus.rest.client.reactive.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "client5")
@Path("/resource")
public interface Client5 {
    @GET
    Response get();
}
