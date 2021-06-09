package io.quarkus.rest.client.reactive.lock.prevention;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
