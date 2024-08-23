package io.quarkus.restclient.exception;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendService {
    @RestClient
    DownstreamServiceClient client;

    @GET
    @Path("/exception")
    public String exception() {
        return client.getData();
    }

    @GET
    @Path("/exception-caught")
    public String exceptionCaught() {
        try {
            return client.getData();
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            throw new WebApplicationException(r);
        }
    }

}
