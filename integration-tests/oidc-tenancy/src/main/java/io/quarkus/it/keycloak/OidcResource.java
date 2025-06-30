package io.quarkus.it.keycloak;

import java.net.URI;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;

import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import io.smallrye.jwt.util.KeyUtils;

@Path("oidc")
public class OidcResource {
    private static final Logger LOG = Logger.getLogger(OidcResource.class);

    @Context
    UriInfo ui;
    RsaJsonWebKey key;
    private volatile boolean introspection;
    private volatile boolean rotate;
    private volatile int jwkEndpointCallCount;
    private volatile int introspectionEndpointCallCount;
    private volatile int opaqueToken2UsageCount;
    private volatile int opaqueToken3UsageCount;
    private volatile int revokeEndpointCallCount;
    private volatile int userInfoEndpointCallCount;
    private volatile boolean enableDiscovery = true;
    private volatile int refreshEndpointCallCount;

    @PostConstruct
    public void init() throws Exception {
        key = RsaJwkGenerator.generateJwk(2048);
        key.setUse("sig");
        key.setKeyId("1");
        key.setAlgorithm("RS256");
    }

    @GET
    @Produces("application/json")
    @Path(".well-known/openid-configuration")
    public String discovery() {
        if (enableDiscovery) {
            final String baseUri = ui.getBaseUriBuilder().path("oidc").build().toString();
            return "{" +
                    "   \"token_endpoint\":" + "\"" + baseUri + "/token\"," +
                    "   \"introspection_endpoint\":" + "\"" + baseUri + "/introspect\"," +
                    "   \"userinfo_endpoint\":" + "\"" + baseUri + "/userinfo\"," +
                    "   \"revocation_endpoint\":" + "\"" + baseUri + "/revoke\"," +
                    "   \"jwks_uri\":" + "\"" + baseUri + "/jwks\"" +
                    "  }";
        } else {
            return "{}";
        }
    }

    @GET
    @Produces("application/json")
    @Path("jwks")
    public String jwks() {
        jwkEndpointCallCount++;
        if (introspection) {
            return "{\"keys\":[]}";
        }
        String json = new JsonWebKeySet(key).toJson();
        if (rotate) {
            json = json.replace("\"1\"", "\"2\"");
        }
        return json;
    }

    @GET
    @Path("jwk-endpoint-call-count")
    public int jwkEndpointCallCount() {
        return jwkEndpointCallCount;
    }

    @POST
    @Path("jwk-endpoint-call-count")
    public int resetJwkEndpointCallCount() {
        jwkEndpointCallCount = 0;
        return jwkEndpointCallCount;
    }

    @GET
    @Path("introspection-endpoint-call-count")
    public int introspectionEndpointCallCount() {
        return introspectionEndpointCallCount;
    }

    @POST
    @Path("introspection-endpoint-call-count")
    public int resetIntrospectionEndpointCallCount() {
        introspectionEndpointCallCount = 0;
        return introspectionEndpointCallCount;
    }

    @POST
    @Path("opaque-token-call-count")
    public int resetOpaqueTokenCallCount() {
        opaqueToken2UsageCount = 0;
        return opaqueToken2UsageCount;
    }

    @POST
    @Path("opaque-token-3-call-count")
    public int resetOpaqueToken3CallCount() {
        opaqueToken3UsageCount = 0;
        return opaqueToken3UsageCount;
    }

    @POST
    @Produces("application/json")
    @Path("introspect")
    public String introspect(@FormParam("client_id") String clientId, @FormParam("client_secret") String clientSecret,
            @HeaderParam("Authorization") String authorization, @FormParam("token") String token) throws Exception {
        introspectionEndpointCallCount++;

        boolean activeStatus = introspection && !token.endsWith("-invalid");
        String requiredClaim = "\"required_claim\": \"1\",";
        if (token.endsWith("_2") && ++opaqueToken2UsageCount == 2) {
            // This is to confirm that the same opaque token_2 works well when its introspection response
            // includes `required_claim` with value "1" but fails when the required claim is not included
            requiredClaim = "";
        }
        if (token.endsWith("_3")) {
            ++opaqueToken3UsageCount;
            // This is to confirm that the same opaque token_3 works well when its introspection response
            // includes `required_claim` with values "1,2" but fails when the required claim is not included
            // or when required claim value is just "1"
            if (opaqueToken3UsageCount == 2) {
                requiredClaim = "\"required_claim\": [\"1\"],";
            }
            if (opaqueToken3UsageCount == 3) {
                requiredClaim = "";
            }
            if (opaqueToken3UsageCount == 4) {
                requiredClaim = "\"required_claim\": [\"1\", \"2\"],";
            }
        }
        String introspectionClientId = "none";
        String introspectionClientSecret = "none";
        if (clientSecret != null) {
            // Secret is expected to be a JWT
            PublicKey verificationKey = KeyUtils.readPublicKey("ecPublicKey.pem", SignatureAlgorithm.ES256);
            JWTParser parser = new DefaultJWTParser();
            // "client-introspection-only" is a client id, set as an issuer by default
            JWTAuthContextInfo contextInfo = new JWTAuthContextInfo(verificationKey, "client-introspection-only");
            contextInfo.setSignatureAlgorithm(Set.of(SignatureAlgorithm.ES256));
            JsonWebToken jwt = parser.parse(clientSecret, contextInfo);
            clientId = jwt.getIssuer();
        } else if (authorization != null) {
            String plainChallenge = new String(Base64.getDecoder().decode(authorization.substring("Basic ".length())));
            int colonPos;
            if ((colonPos = plainChallenge.indexOf(":")) > -1) {
                introspectionClientId = plainChallenge.substring(0, colonPos);
                introspectionClientSecret = plainChallenge.substring(colonPos + 1);
            }
        }

        return "{" +
                "   \"active\": " + activeStatus + "," +
                "   \"scope\": \"user\"," +
                "   \"email\": \"user@gmail.com\"," +
                "   \"username\": \"alice\"," +
                requiredClaim +
                "   \"introspection_client_id\": \"" + introspectionClientId + "\"," +
                "   \"introspection_client_secret\": \"" + introspectionClientSecret + "\"," +
                "   \"client_id\": \"" + clientId + "\"" +
                "  }";
    }

    @GET
    @Path("revoke-endpoint-call-count")
    public int revokeEndpointCallCount() {
        return revokeEndpointCallCount;
    }

    @POST
    @Path("revoke-endpoint-call-count")
    public int resetRevokeEndpointCallCount() {
        revokeEndpointCallCount = 0;
        return revokeEndpointCallCount;
    }

    @POST
    @Path("revoke")
    public void revoke(@FormParam("token") String token) throws Exception {
        if (token != null) {
            revokeEndpointCallCount++;
        }
    }

    @GET
    @Path("userinfo-endpoint-call-count")
    public int userInfoEndpointCallCount() {
        return userInfoEndpointCallCount;
    }

    @POST
    @Path("userinfo-endpoint-call-count")
    public int resetUserInfoEndpointCallCount() {
        userInfoEndpointCallCount = 0;
        return userInfoEndpointCallCount;
    }

    @GET
    @Produces("application/json")
    @Path("userinfo")
    public String userinfo() {
        userInfoEndpointCallCount++;

        return "{" +
                "   \"sub\": \"123456789\"," +
                "   \"preferred_username\": \"alice\"" +
                "  }";
    }

    @POST
    @Path("token")
    @Produces("application/json")
    public String token(@FormParam("grant_type") String grantType, @FormParam("client_id") String clientId) {
        if ("authorization_code".equals(grantType)) {
            return "{\"id_token\": \"" + jwt(clientId, null, "1") + "\"," +
                    "\"access_token\": \"" + jwt(clientId, null, "1") + "\"," +
                    "   \"token_type\": \"Bearer\"," +
                    "   \"refresh_token\": \"123456789\"," +
                    "   \"expires_in\": 300 }";
        } else if ("refresh_token".equals(grantType)) {
            // Emulate the case where the provider returns the refresh token only once
            // and does not recycle refresh tokens during  the refresh token grant request.

            if (refreshEndpointCallCount++ == 0) {
                // first refresh token request, check the original ID token is used
                return "{\"access_token\": \"" + jwt(clientId, null, "1") + "\"," +
                        "   \"token_type\": \"Bearer\"," +
                        "   \"expires_in\": 300 }";
            } else {
                // force an error to test the case where the refresh token eventually becomes invalid
                // quarkus-oidc should redirect the user to authenticate again if refreshing the token fails
                throw new BadRequestException();
            }
        } else {
            // unexpected grant request
            throw new BadRequestException();
        }
    }

    @POST
    @Path("accesstoken-with-acr")
    @Produces("application/json")
    public String testAccessTokenWithAcr(@QueryParam("acr") String acr, @QueryParam("auth_time") String authTime) {
        return "{\"access_token\": \"" + jwt(null, "123456789", "1", false, acr, authTime)
                + "\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("accesstoken")
    @Produces("application/json")
    public String testAccessToken(@QueryParam("kid") String kid, @QueryParam("sub") String subject) {
        return "{\"access_token\": \"" + jwt(null, subject, kid) + "\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("accesstoken-empty-scope")
    @Produces("application/json")
    public String testAccessTokenWithEmptyScope(@QueryParam("kid") String kid, @QueryParam("sub") String subject) {
        return "{\"access_token\": \"" + jwt(null, subject, kid, true) + "\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("opaque-token")
    @Produces("application/json")
    public String testOpaqueToken() {
        return "{\"access_token\": \"987654321\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("opaque-token2")
    @Produces("application/json")
    public String testOpaqueToken2() {
        return "{\"access_token\": \"987654321_2\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("opaque-token3")
    @Produces("application/json")
    public String testOpaqueToken3() {
        return "{\"access_token\": \"987654321_3\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("enable-introspection")
    public boolean setIntrospection() {
        introspection = true;
        return introspection;
    }

    @POST
    @Path("disable-introspection")
    public boolean disableIntrospection() {
        introspection = false;
        return introspection;
    }

    @POST
    @Path("enable-discovery")
    public boolean setDiscovery() {
        enableDiscovery = true;
        return enableDiscovery;
    }

    @POST
    @Path("disable-discovery")
    public boolean disableDiscovery() {
        enableDiscovery = false;
        return enableDiscovery;
    }

    @POST
    @Path("enable-rotate")
    public boolean setRotate() {
        rotate = true;
        return rotate;
    }

    @POST
    @Path("disable-rotate")
    public boolean disableRotate() {
        rotate = false;
        return rotate;
    }

    @POST
    @Path("form-post-logout")
    public Response formPostLogout(@FormParam("id_token_hint") String idTokenHint,
            @FormParam("post_logout_redirect_uri") String postLogoutRedirectUri) throws Exception {
        String userName = OidcUtils.decodeJwtContent(idTokenHint).getString("upn");
        LOG.infof("OIDC provider: logging the user out using the form post logout. post logout redirect URI: %s",
                postLogoutRedirectUri);
        return Response.seeOther(URI.create(postLogoutRedirectUri + "?username=" + userName)).build();
    }

    private String jwt(String audience, String subject, String kid) {
        return jwt(audience, subject, kid, false);
    }

    private String jwt(String audience, String subject, String kid, boolean withEmptyScope) {
        return jwt(audience, subject, kid, withEmptyScope, null, null);
    }

    private String jwt(String audience, String subject, String kid, boolean withEmptyScope, String acr, String authTime) {
        JwtClaimsBuilder builder = Jwt.claim("typ", "Bearer")
                .upn("alice")
                .preferredUserName("alice")
                .groups("user")
                .expiresIn(Duration.ofSeconds(4));
        if (audience != null) {
            builder.audience(audience);
        }
        if (subject != null) {
            builder.subject(subject);
        }

        if (withEmptyScope) {
            builder.claim("scope", "");
        }

        if (acr != null && !acr.isEmpty()) {
            builder.claim("acr", Arrays.asList(acr.split(",")));
        }

        if (authTime != null && !authTime.isEmpty()) {
            builder.claim("auth_time", Long.parseLong(authTime));
        }

        return builder.jws().keyId(kid)
                .sign(key.getPrivateKey());
    }
}
