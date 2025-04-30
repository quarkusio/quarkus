package io.quarkus.rest.client.reactive.registerclientheaders;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.common.annotation.Blocking;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/")
@ApplicationScoped
public class TestResource {

    @RestClient
    HeaderPassingClient headerPassingClient;
    @RestClient
    HeaderNoPassingClient headerNoPassingClient;

    @GET
    @Path("/echo")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String echo(@QueryParam("message") String message,
            @HeaderParam("foo") String foo) {
        return message + (foo == null ? "_null_" : foo);
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
        return headerPassingClient.call();
    }

    @GET
    @Path("/with-incoming-header/no-passing")
    @Blocking
    public RequestData callThroughNotPassingClient() {
        return headerNoPassingClient.call();
    }

}
