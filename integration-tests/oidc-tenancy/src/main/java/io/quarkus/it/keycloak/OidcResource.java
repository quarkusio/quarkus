package io.quarkus.it.keycloak;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;

import io.smallrye.jwt.build.Jwt;

@Path("oidc")
public class OidcResource {

    @Context
    UriInfo ui;
    RsaJsonWebKey key;
    private volatile boolean introspection;
    private volatile boolean rotate;
    private volatile int jwkEndpointCallCount;

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
        final String baseUri = ui.getBaseUriBuilder().path("oidc").build().toString();
        return "{" +
                "   \"token_endpoint\":" + "\"" + baseUri + "/token\"," +
                "   \"token_introspection_endpoint\":" + "\"" + baseUri + "/introspect\"," +
                "   \"jwks_uri\":" + "\"" + baseUri + "/jwks\"" +
                "  }";
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

    @POST
    @Produces("application/json")
    @Path("introspect")
    public String introspect() {
        // Introspect call will return an active token status only when the introspection is disallowed.
        // This is done to test that an asynchronous JWK refresh call done by Vertx Auth is effective.
        return "{" +
                "   \"active\": " + introspection + "," +
                "   \"scope\": \"user\"," +
                "   \"username\": \"alice\"" +
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
    @Path("introspection")
    public boolean setIntrospection() {
        introspection = true;
        return introspection;
    }

    @GET
    @Path("introspection-status")
    public boolean introspectionStatus() {
        return introspection;
    }

    @POST
    @Path("rotate")
    public boolean setRotate() {
        rotate = true;
        return rotate;
    }

    @GET
    @Path("rotate-status")
    public boolean rotateStatus() {
        return rotate;
    }

    private String jwt(String kid) {
        return Jwt.claims()
                .claim("typ", "Bearer")
                .upn("alice")
                .preferredUserName("alice")
                .groups("user")
                .jws().signatureKeyId(kid)
                .sign(key.getPrivateKey());
    }
}
