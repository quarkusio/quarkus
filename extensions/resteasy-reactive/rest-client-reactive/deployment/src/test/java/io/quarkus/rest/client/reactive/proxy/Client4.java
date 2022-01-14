package io.quarkus.rest.client.reactive.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "client4")
@Path("/resource")
public interface Client4 {
    @GET
    Response get();
}