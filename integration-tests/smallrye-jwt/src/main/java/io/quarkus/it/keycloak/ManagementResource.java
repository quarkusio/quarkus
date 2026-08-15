package io.quarkus.it.keycloak;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.jws.JsonWebSignature;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;

@Path("/management")
public class ManagementResource {

    @Inject
    JWTParser parser;

    @ConfigProperty(name = "smallrye.jwt.new-token.issuer")
    String issuer;

    @GET
    @Path("/only-user")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ "USER" })
    public String hello() {
        return "User Panel ::: Management";
    }

    /**
     * Signs a token with a generated Ed25519 key and verifies it, to confirm the EdDSA support works at runtime,
     * including in native mode.
     */
    @GET
    @Path("/eddsa")
    @Produces(MediaType.TEXT_PLAIN)
    public String signAndVerifyWithEdDsa() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

        String token = Jwt.upn("alice@quarkus.io").jws().algorithm(SignatureAlgorithm.EDDSA).sign(keyPair.getPrivate());
        String algorithm = JsonWebSignature.parse(token).headers().algorithm();

        JsonWebToken jwt = parser.parse(token, edDsaContextInfo(keyPair));

        // the token must not verify with another Ed25519 public key
        KeyPair otherKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        try {
            parser.parse(token, edDsaContextInfo(otherKeyPair));
            return "verified with a wrong key";
        } catch (ParseException ex) {
            // expected
        }

        return algorithm + ":" + jwt.getName();
    }

    private JWTAuthContextInfo edDsaContextInfo(KeyPair keyPair) {
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo(keyPair.getPublic(), issuer);
        contextInfo.setSignatureAlgorithm(Set.of(SignatureAlgorithm.EDDSA));
        return contextInfo;
    }
}