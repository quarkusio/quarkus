package io.quarkus.jwt.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.jwt.Claims;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

public class JwtBuildUnitTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("application.properties"));

    @Test
    public void signToken() throws Exception {
        String jwt = Jwt.preferredUserName("alice").sign();

        JwtClaims jwtClaims = new JwtConsumerBuilder()
                .setVerificationKey(KeyUtils.readPublicKey("/publicKey.pem"))
                .build()
                .processToClaims(jwt);
        assertEquals("alice", jwtClaims.getClaimValue(Claims.preferred_username.name()));

    }
}
