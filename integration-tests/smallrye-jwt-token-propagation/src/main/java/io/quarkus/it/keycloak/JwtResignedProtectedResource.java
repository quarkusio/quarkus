package io.quarkus.it.keycloak;

import java.security.PublicKey;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.util.KeyUtils;

@Path("/jwt-resigned-protected")
public class JwtResignedProtectedResource {

    JWTParser parser;

    @PostConstruct
    public void loadVerificationKey() throws Exception {
        PublicKey verificationKey = KeyUtils.readPublicKey("/publicKey.pem");
        parser = new DefaultJWTParser(new JWTAuthContextInfo(verificationKey, "http://frontend-resource"));
    }

    @GET
    public String principalName(@HeaderParam("Authorization") String authorization) throws Exception {
        JsonWebToken jwt = parser.parse(authorization.split(" ")[1]);
        checkIssuerAndAudience(jwt);
        return jwt.getName();
    }

    private void checkIssuerAndAudience(JsonWebToken jwt) {
        if (!"http://frontend-resource".equals(jwt.getIssuer())) {
            throw new NotAuthorizedException(401);
        }
        Set<String> aud = jwt.getAudience();
        if (aud.size() != 1 || !aud.contains("http://jwt-resigned-protected-resource")) {
            throw new NotAuthorizedException(401);
        }
    }
}
