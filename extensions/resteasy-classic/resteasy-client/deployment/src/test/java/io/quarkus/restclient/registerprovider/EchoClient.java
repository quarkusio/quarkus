package io.quarkus.restclient.registerprovider;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/echo")
@RegisterRestClient
@RegisterProvider(MyFilter.class)
public interface EchoClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    String echo(@QueryParam("message") String message);

    @Path("call-client")
    @GET
    String callClient();

    @Path("called-from-client")
    @GET
    String calledFromClient(@QueryParam("uniqueNumber") int uniqueNumber);

    @Path("async/call-client")
    @GET
    CompletionStage<String> asyncCallClient();

    @Path("async/called-from-client")
    @GET
    CompletionStage<String> asyncCalledFromClient(@QueryParam("uniqueNumber") int uniqueNumber);
}
