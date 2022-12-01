package org.acme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.acme.CustomHeader1;
import org.acme.CustomHeader2;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@CustomHeader1
@CustomHeader2
@Path("/")
public interface DownstreamServiceClient {

    @GET
    String getHeaders();
}
