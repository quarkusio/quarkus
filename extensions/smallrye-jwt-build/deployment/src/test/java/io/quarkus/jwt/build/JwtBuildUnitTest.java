package io.quarkus.jwt.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.jws.JwsVerifier;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.common.JwtClaims;
import io.smallrye.jwt.util.KeyUtils;

public class JwtBuildUnitTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("application.properties"));

    @Test
    public void signToken() throws Exception {
        String jwt = Jwt.preferredUserName("alice").sign();

        String payload = JwsVerifier.builder(KeyUtils.readPublicKey("/publicKey.pem"))
                .algorithm(SignatureAlgorithm.RS256).build().verify(jwt);
        JwtClaims jwtClaims = JwtClaims.parse(payload);
        assertEquals("alice", jwtClaims.getClaim(Claims.preferred_username.name()));

    }
}
