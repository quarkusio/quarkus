package io.quarkus.oidc.client.filter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
