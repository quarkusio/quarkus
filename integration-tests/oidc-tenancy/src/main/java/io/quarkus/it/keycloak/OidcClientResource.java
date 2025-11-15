package io.quarkus.it.keycloak;

import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Path("/oidc-client")
public class OidcClientResource {

    @Inject
    OidcClient oidcClient;

    @GET
    @Path("/multiple-audiences")
    public String getAudiences() {
        String accessTokenJson = oidcClient.getTokens().await().indefinitely().getAccessToken();
        JsonObject json = OidcUtils.decodeJwtContent(accessTokenJson);
        JsonArray audJson = json.getJsonArray("aud");

        return audJson.stream().map(aud -> (String) aud).sorted().collect(Collectors.joining(","));
    }
}
