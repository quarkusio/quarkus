package io.quarkus.it.rest.client.http2;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl;

import io.quarkus.vertx.http.HttpServer;

@Path("")
public class Resource {

    private final Client client;
    private final Client2 client2;

    @Inject
    public Resource(HttpServer httpServer, @RestClient Client client) {
        this.client = client;
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

    @GET
    @Path("/ping")
    public String ping() {
        return "pong";
    }
}
