package io.quarkus.restclient.jaxb.deployment;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/client")
public class ClientResource {

    @Inject
    @RestClient
    RestInterface restInterface;

    @GET
    @Path("/hello")
    public String hello() {
        Book book = restInterface.get();

        if ("L'axe du loup".equals(book.getTitle())) {
            return "OK";
        }

        return "INVALID";
    }
}
