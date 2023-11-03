package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "access-token-propagation")
@RegisterProvider(CustomAccessTokenRequestFilter.class)
@Path("/")
public interface AccessTokenPropagationService {

    @GET
    String getUserName();
}
