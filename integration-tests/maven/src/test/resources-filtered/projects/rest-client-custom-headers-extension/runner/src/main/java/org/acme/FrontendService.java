package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/frontend")
public class FrontendService {
    @Inject
    @RestClient
    DownstreamServiceClient client;

    @GET
    public String getHeaders() {
        return client.getHeaders();
    }
}
