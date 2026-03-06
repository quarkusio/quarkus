package io.quarkus.it.rest.client.main;

import java.net.URI;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.ClientRedirectHandler;

@RegisterRestClient(configKey = "remote-resource")
public interface RedirectResourceClient {
    @ClientRedirectHandler
    static URI redirect(Response response) {
        if (Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.REDIRECTION) {
            return response.getLocation();
        }
        return null;
    }

    @POST
    @Path("/redirect/response")
    String redirectResponse();
}
