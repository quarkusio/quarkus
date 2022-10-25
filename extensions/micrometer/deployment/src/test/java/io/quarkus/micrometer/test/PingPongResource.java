package io.quarkus.micrometer.test;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

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
    public Response pong(@PathParam("message") String message) {
        if (message.equals("500")) {
            return Response.status(500).build();
        } else if (message.equals("400")) {
            return Response.status(400).build();
        }
        return Response.ok(message, "text/plain").build();
    }

    @GET
    @Path("ping/{message}")
    public String ping(@PathParam("message") String message) {
        try {
            return pingRestClient.pingpong(message);
        } catch (Exception ex) {
            if (!"400".equals(message) && !"500".equals(message)) {
                throw ex;
            }
            // expected exception
        }
        return message;
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
