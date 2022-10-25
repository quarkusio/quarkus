package io.quarkus.it.keycloak;

import java.security.PublicKey;
import java.time.Duration;
import java.util.Base64;

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
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

@Path("oidc")
public class OidcResource {

    @Context
    UriInfo ui;
    RsaJsonWebKey key;
    private volatile boolean introspection;
    private volatile boolean rotate;
    private volatile int jwkEndpointCallCount;
    private volatile int introspectionEndpointCallCount;
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
    @Produces("application/json")
    @Path("introspect")
    public String introspect(@FormParam("client_id") String clientId, @FormParam("client_secret") String clientSecret,
            @HeaderParam("Authorization") String authorization) throws Exception {
        introspectionEndpointCallCount++;

        String introspectionClientId = "none";
        String introspectionClientSecret = "none";
        if (clientSecret != null) {
            // Secret is expected to be a JWT
            PublicKey verificationKey = KeyUtils.readPublicKey("ecPublicKey.pem", SignatureAlgorithm.ES256);
            JWTParser parser = new DefaultJWTParser();
            // "client-introspection-only" is a client id, set as an issuer by default
            JWTAuthContextInfo contextInfo = new JWTAuthContextInfo(verificationKey, "client-introspection-only");
            contextInfo.setSignatureAlgorithm(SignatureAlgorithm.ES256);
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
                "   \"active\": " + introspection + "," +
                "   \"scope\": \"user\"," +
                "   \"email\": \"user@gmail.com\"," +
                "   \"username\": \"alice\"," +
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
                "   \"preferred_username\": \"alice\"" +
                "  }";
    }

    @POST
    @Path("token")
    @Produces("application/json")
    public String token(@FormParam("grant_type") String grantType) {
        if ("authorization_code".equals(grantType)) {
            return "{\"id_token\": \"" + jwt("1") + "\"," +
                    "\"access_token\": \"" + jwt("1") + "\"," +
                    "   \"token_type\": \"Bearer\"," +
                    "   \"refresh_token\": \"123456789\"," +
                    "   \"expires_in\": 300 }";
        } else if ("refresh_token".equals(grantType)) {
            // Emulate the case where the provider returns the refresh token only once
            // and does not recycle refresh tokens during  the refresh token grant request.

            if (refreshEndpointCallCount++ == 0) {
                // first refresh token request
                return "{\"id_token\": \"" + jwt("1") + "\"," +
                        "\"access_token\": \"" + jwt("1") + "\"," +
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
    @Path("accesstoken")
    @Produces("application/json")
    public String testAccessToken(@QueryParam("kid") String kid) {
        return "{\"access_token\": \"" + jwt(kid) + "\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("opaque-token")
    @Produces("application/json")
    public String testOpaqueToken(@QueryParam("kid") String kid) {
        return "{\"access_token\": \"987654321\"," +
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

    private String jwt(String kid) {
        return Jwt.claims()
                .claim("typ", "Bearer")
                .upn("alice")
                .preferredUserName("alice")
                .groups("user")
                .expiresIn(Duration.ofSeconds(4))
                .jws().keyId(kid)
                .sign(key.getPrivateKey());
    }
}
