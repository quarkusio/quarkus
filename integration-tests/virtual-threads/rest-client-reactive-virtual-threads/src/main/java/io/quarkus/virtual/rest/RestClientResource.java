package io.quarkus.virtual.rest;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.common.annotation.RunOnVirtualThread;

@Path("/")
@RunOnVirtualThread
public class RestClientResource {

    @RestClient
    ServiceClient client;

    @Inject
    @VirtualThreads
    ExecutorService executor;

    @GET
    public Greeting test() {
        VirtualThreadsAssertions.assertEverything();
        assert executor != null;
        return client.hello();
    }

}
