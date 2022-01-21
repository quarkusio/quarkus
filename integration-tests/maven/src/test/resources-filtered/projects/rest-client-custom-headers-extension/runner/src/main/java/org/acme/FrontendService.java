package org.acme;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
