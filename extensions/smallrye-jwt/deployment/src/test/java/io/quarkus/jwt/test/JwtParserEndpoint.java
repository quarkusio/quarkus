package io.quarkus.jwt.test;

import java.security.PublicKey;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

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
