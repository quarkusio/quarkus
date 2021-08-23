package io.quarkus.restclient.registerprovider;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/echo")
@RegisterRestClient
@RegisterProvider(MyFilter.class)
@ApplicationScoped
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
