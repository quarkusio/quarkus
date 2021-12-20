package io.quarkus.it.keycloak;

import java.security.PublicKey;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

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
    private volatile int userInfoEndpointCallCount;
    private volatile boolean enableDiscovery = true;

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
    public String introspect(@FormParam("client_secret") String secret) throws Exception {
        introspectionEndpointCallCount++;

        String clientId = "undefined";
        if (secret != null) {
            // Secret is expected to be a JWT
            PublicKey verificationKey = KeyUtils.readPublicKey("ecPublicKey.pem", SignatureAlgorithm.ES256);
            JWTParser parser = new DefaultJWTParser();
            // "client-introspection-only" is a client id, set as an issuer by default
            JWTAuthContextInfo contextInfo = new JWTAuthContextInfo(verificationKey, "client-introspection-only");
            contextInfo.setSignatureAlgorithm(SignatureAlgorithm.ES256);
            JsonWebToken jwt = parser.parse(secret, contextInfo);
            clientId = jwt.getIssuer();
        }

        return "{" +
                "   \"active\": " + introspection + "," +
                "   \"scope\": \"user\"," +
                "   \"email\": \"user@gmail.com\"," +
                "   \"username\": \"alice\"," +
                "   \"client_id\": \"" + clientId + "\"" +
                "  }";
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
    public String token(@QueryParam("kid") String kid) {
        return "{\"access_token\": \"" + jwt(kid) + "\"," +
                "   \"token_type\": \"Bearer\"," +
                "   \"refresh_token\": \"123456789\"," +
                "   \"expires_in\": 300 }";
    }

    @POST
    @Path("opaque-token")
    @Produces("application/json")
    public String opaqueToken(@QueryParam("kid") String kid) {
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
                .jws().keyId(kid)
                .sign(key.getPrivateKey());
    }
}
