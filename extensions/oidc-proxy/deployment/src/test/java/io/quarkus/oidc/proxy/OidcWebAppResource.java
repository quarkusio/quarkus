package io.quarkus.oidc.proxy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

@Path("/web-app")
@Authenticated
public class OidcWebAppResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    @RestClient
    ServiceApiClient serviceApiClient;

    @GET
    @Produces("text/plain")
    public Uni<String> getName() {
        return serviceApiClient.getName().onItem()
                .transform(c -> ("web-app: " + idToken.getClaim("typ") + " " + idToken.getName() + ", service: " + c));
    }
}
