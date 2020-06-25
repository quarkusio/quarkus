package io.quarkus.it.rest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/test")
@RegisterClientHeaders
public interface RestInterface {

    @GET
    String get();

    @GET
    @Path("/echo/{echo}")
    String echo(@PathParam("echo") String echo);

    @GET
    CompletionStage<String> asyncGet();

    @GET
    @Path("/jackson")
    @Produces("application/json")
    CompletionStage<TestResource.MyData> getDataAsync();

    @GET
    @Path("/complex")
    @Produces("application/json")
    List<ComponentType> complex();

    @GET
    @Path("/headers")
    @Produces("application/json")
    Map<String, String> getAllHeaders();
}
