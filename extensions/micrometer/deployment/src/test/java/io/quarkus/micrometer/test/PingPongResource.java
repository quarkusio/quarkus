package io.quarkus.micrometer.test;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/")
@Singleton
public class PingPongResource {
    @RegisterRestClient(configKey = "pingpong")
    public interface PingPongRestClient {
        @GET
        @Path("pong/{message}")
        String pingpong(@PathParam("message") String message);

        @GET
        @Path("pong/{message}")
        CompletionStage<String> asyncPingPong(@PathParam("message") String message);
    }

    @Inject
    @RestClient
    PingPongRestClient pingRestClient;

    @GET
    @Path("pong/{message}")
    public String pong(@PathParam("message") String message) {
        return message;
    }

    @GET
    @Path("ping/{message}")
    public String ping(@PathParam("message") String message) {
        return pingRestClient.pingpong(message);
    }

    @GET
    @Path("async-ping/{message}")
    public CompletionStage<String> asyncPing(@PathParam("message") String message) {
        return pingRestClient.asyncPingPong(message);
    }

    @GET
    @Path("one")
    public String one() {
        return "OK";
    }

    @GET
    @Path("two")
    public String two() {
        return "OK";
    }
}
