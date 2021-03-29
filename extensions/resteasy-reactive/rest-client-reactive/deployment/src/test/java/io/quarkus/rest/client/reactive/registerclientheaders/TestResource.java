package io.quarkus.rest.client.reactive.registerclientheaders;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.common.annotation.Blocking;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/")
@ApplicationScoped
public class TestResource {

    @RestClient
    HeaderPassingClient headerPassingClient;

    @GET
    @Path("/echo")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String echo(@QueryParam("message") String message,
            @HeaderParam("foo") String foo) {
        return message + foo;
    }

    @GET
    @Path("/describe-request")
    public RequestData describeRequest(@Context HttpHeaders headers) {
        RequestData result = new RequestData();
        result.setHeaders(headers.getRequestHeaders());
        return result;
    }

    @GET
    @Path("/with-incoming-header")
    @Blocking
    public RequestData callThroughClient() {
        RequestData data = headerPassingClient.call();
        return data;
    }

}
