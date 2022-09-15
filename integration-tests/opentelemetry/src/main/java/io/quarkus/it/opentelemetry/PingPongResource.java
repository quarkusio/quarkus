package io.quarkus.it.opentelemetry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;

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
    }

    @Inject
    @RestClient
    PingPongRestClient pingRestClient;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "test.url")
    String testUrl;

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
    @Path("async-ping-named/{message}")
    public Uni<String> asyncPingNamed(@PathParam("message") String message) {
        HttpClient httpClient = vertx.createHttpClient();
        RequestOptions options = new RequestOptions()
                .setMethod(HttpMethod.GET)
                .setAbsoluteURI(testUrl + "/client/pong/" + message)
                .setHeaders(MultiMap.caseInsensitiveMultiMap())
                .setTraceOperation("Async Ping");
        return httpClient.request(options)
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .onItemOrFailure().call(httpClient::close);
    }

}
