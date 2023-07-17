package io.quarkus.rest.client.reactive.lock.prevention;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/echo")
@RegisterRestClient
@Produces("text/plain")
@Consumes("text/plain")
public interface TestClient {

    @GET
    String blockingCall();

    @GET
    CompletionStage<String> nonBlockingCall();
}
