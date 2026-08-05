package io.quarkus.grpc.examples.interceptors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ping")
@Path("/hello")
public interface PingRestClient {

    @GET
    @Path("/ping")
    String ping();
}
