package io.quarkus.oidc.client.filter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
