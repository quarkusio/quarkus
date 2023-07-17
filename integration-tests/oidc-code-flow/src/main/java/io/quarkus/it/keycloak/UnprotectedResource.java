package io.quarkus.it.keycloak;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcUtils;

@Path("/public-web-app")
public class UnprotectedResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    OidcConfigurationMetadata configMetadata;

    @Inject
    OidcClient oidcClient;

    @Context
    UriInfo ui;

    @GET
    @Path("name")
    public String getName() {
        return idToken.getName() == null ? "no user" : idToken.getName();
    }

    @GET
    @Path("callback")
    public String callback(@QueryParam("code") String code) {
        String redirectUriParam = ui.getBaseUriBuilder().path("public-web-app/callback").build().toString();

        Map<String, String> grantParams = new HashMap<>();
        grantParams.put(OidcConstants.CODE_FLOW_CODE, code);
        grantParams.put(OidcConstants.CODE_FLOW_REDIRECT_URI, redirectUriParam);
        String encodedIdToken = oidcClient.getTokens(grantParams).await().indefinitely().get(OidcConstants.ID_TOKEN_VALUE);
        return OidcUtils.decodeJwtContent(encodedIdToken).getString("preferred_username");
    }

    @GET
    @Path("login")
    public Response login() {
        StringBuilder codeFlowParams = new StringBuilder();

        // response_type
        codeFlowParams.append(OidcConstants.CODE_FLOW_RESPONSE_TYPE).append("=").append(OidcConstants.CODE_FLOW_CODE);
        // client_id
        codeFlowParams.append("&").append(OidcConstants.CLIENT_ID).append("=").append("quarkus-app");
        // scope
        codeFlowParams.append("&").append(OidcConstants.TOKEN_SCOPE).append("=").append("openid");
        // redirect_uri
        String redirectUriParam = ui.getBaseUriBuilder().path("public-web-app/callback").build().toString();
        codeFlowParams.append("&").append(OidcConstants.CODE_FLOW_REDIRECT_URI).append("=")
                .append(OidcCommonUtils.urlEncode(redirectUriParam));
        // state
        String state = UUID.randomUUID().toString();
        codeFlowParams.append("&").append(OidcConstants.CODE_FLOW_STATE).append("=").append(state);
        return Response.seeOther(URI.create(configMetadata.getAuthorizationUri() + "?" + codeFlowParams.toString()))
                .cookie(new NewCookie("state", state))
                .build();
    }
}
