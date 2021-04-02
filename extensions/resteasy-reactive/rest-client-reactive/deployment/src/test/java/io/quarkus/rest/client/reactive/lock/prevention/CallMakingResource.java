package io.quarkus.rest.client.reactive.lock.prevention;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.common.annotation.Blocking;

@Path("/non-blocking")
public class CallMakingResource {

    @RestClient
    TestClient client;

    @GET
    @Path("/block")
    public String doABlockingCall() {
        return client.blockingCall();
    }

    @GET
    @Path("/non-block")
    public CompletionStage<String> doANonBlockingCall() {
        return client.nonBlockingCall();
    }

    @GET
    @Path("/block-properly")
    @Blocking
    public String doABlockingCallFromBlocking() {
        return client.blockingCall();
    }
}
