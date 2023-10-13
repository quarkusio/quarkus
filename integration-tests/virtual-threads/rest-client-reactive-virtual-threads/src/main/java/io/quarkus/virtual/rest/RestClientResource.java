package io.quarkus.virtual.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/")
@RunOnVirtualThread
public class RestClientResource {

    @RestClient
    ServiceClient client;

    @GET
    public Greeting test() {
        VirtualThreadsAssertions.assertEverything();
        return client.hello();
    }

}
