package io.quarkus.it.rest.client.http2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl;

@Path("")
public class Resource {

    @RestClient
    Client client;

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
    @Path("/ping")
    public String ping() {
        return "pong";
    }
}
