package io.quarkus.it.rest.client.http2;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl;

import io.quarkus.vertx.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

@Path("")
public class Resource {

    private final Client client;
    private final Client2 client2;
    private final EmptyPathClient emptyPathClient;
    private final EmptyPathHttp1Client emptyPathHttp1Client;

    @Inject
    public Resource(HttpServer httpServer, @RestClient Client client, @RestClient EmptyPathClient emptyPathClient,
            @RestClient EmptyPathHttp1Client emptyPathHttp1Client) {
        this.client = client;
        this.emptyPathClient = emptyPathClient;
        this.emptyPathHttp1Client = emptyPathHttp1Client;
        this.client2 = RestClientBuilder.newBuilder()
                .baseUri(httpServer.getLocalBaseUri())
                .build(Client2.class);
    }

    @GET
    @Path("/client/ping")
    public Response client() {
        Response response = client.ping();
        if (((ClientResponseImpl) response).getHttpVersion().equals("HTTP_2")) {
            return Response.ok(response.readEntity(String.class)).build();
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/client2/ping")
    public Response client2() {
        Response response = client2.ping();
        if (((ClientResponseImpl) response).getHttpVersion().equals("HTTP_2")) {
            return Response.ok(response.readEntity(String.class)).build();
        }

        return Response.noContent().build();
    }

    /**
     * Calls the root of the server three times through one HTTP/2 client with an empty path. The answers carry the
     * HTTP version and the client port the server saw, so the test can check that every request went over the same
     * HTTP/2 connection.
     */
    @GET
    @Path("/client/empty-path")
    public String clientWithEmptyPath() {
        return emptyPathClient.root() + ";" + emptyPathClient.root() + ";" + emptyPathClient.root();
    }

    @GET
    @Path("/client/empty-path-http1")
    public String clientWithEmptyPathOverHttp1() {
        return emptyPathHttp1Client.root();
    }

    @GET
    @Path("/ping")
    public String ping() {
        return "pong";
    }

    /**
     * The root of the server: the HTTP version and the client port of the request, as the server sees them.
     */
    @GET
    public String root(@Context RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        return request.version().name() + " " + request.remoteAddress().port();
    }
}
