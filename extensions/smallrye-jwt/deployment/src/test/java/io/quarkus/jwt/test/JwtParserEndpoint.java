package io.quarkus.jwt.test;

import java.security.PublicKey;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.util.KeyUtils;

@Path("/parser")
@ApplicationScoped
public class JwtParserEndpoint {

    @Inject
    JWTParser parser;

    @GET
    @Path("/name")
    public String tokenWithoutIssuedAt(@HeaderParam("Authorization") String authorization) throws Exception {
        String rawToken = authorization.split(" ")[1].trim();
        return parser.parse(rawToken).getName();
    }

    @GET
    @Path("/name-with-key")
    public String tokenWithoutIssuedAtWithKey(@HeaderParam("Authorization") String authorization) throws Exception {
        String rawToken = authorization.split(" ")[1].trim();
        PublicKey key = KeyUtils.readPublicKey("publicKey.pem");
        return parser.verify(rawToken, key).getName();
    }
}
