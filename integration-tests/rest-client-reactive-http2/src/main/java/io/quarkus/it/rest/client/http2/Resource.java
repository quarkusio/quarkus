package io.quarkus.it.rest.client.http2;

import java.net.MalformedURLException;
import java.net.URL;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl;

@Path("")
public class Resource {

    private final Client client;
    private final Client2 client2;

    public Resource(@ConfigProperty(name = "test.url") String testUrl, @RestClient Client client) {
        this.client = client;
        try {
            this.client2 = RestClientBuilder.newBuilder()
                    .baseUrl(new URL(testUrl))
                    .build(Client2.class);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/client/ping")
    public Response client() {
        Response response = client.ping();
        if (((ClientResponseImpl) response).getHttpVersion().equals("HTTP_2")) {
            return response;
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/client2/ping")
    public Response client2() {
        Response response = client2.ping();
        if (((ClientResponseImpl) response).getHttpVersion().equals("HTTP_2")) {
            return response;
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/ping")
    public String ping() {
        return "pong";
    }
}
