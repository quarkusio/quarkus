package io.quarkus.it.micrometer.prometheus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@Singleton
@Path("/client")
public class PingPongResource {
    @RegisterRestClient(configKey = "pingpong")
    public interface PingPongRestClient {

        @Path("/client/pong/{message}")
        @GET
        String pingpong(@PathParam("message") String message);

        @GET
        @Path("/client/pong/{message}")
        Uni<String> asyncPingpong(@PathParam("message") String message);

        @GET
        @Path("/client/status/{statusCode}")
        public void call(@RestPath int statusCode);

        @GET
        @Path("/client/status/{statusCode}/{sleep}")
        public void call(@RestPath int statusCode, @RestPath int sleep);
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
    @Blocking
    @Path("ping/{message}")
    public String ping(@PathParam("message") String message) {
        return pingRestClient.pingpong(message);
    }

    @GET
    @Path("async-ping/{message}")
    public Uni<String> asyncPing(@PathParam("message") String message) {
        return pingRestClient.asyncPingpong(message);
    }

    @GET
    @Path("status")
    public String clientStatus() {
        String result = "";
        pingRestClient.call(200);
        result += "ok";
        try {
            pingRestClient.call(400);
        } catch (Exception ignored) {
            result += "400";
        }
        try {
            pingRestClient.call(500);
        } catch (Exception ignored) {
            result += "500";
        }
        try {
            pingRestClient.call(200, 5000);
        } catch (Exception ignored) {
            result += "timeout";
        }
        return result;
    }

    @GET
    @Path("status/{statusCode}")
    public Response testStatus(@RestPath int statusCode) {
        return Response.status(statusCode).build();
    }

    @GET
    @Path("status/{statusCode}/{sleep}")
    public Response testStatusSleep(@RestPath int statusCode, @RestPath int sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ignored) {
        }
        return Response.status(statusCode).build();
    }
}
