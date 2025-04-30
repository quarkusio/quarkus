package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

public class ClaimsSingletonTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ClaimsSingletonEndpoint.class)
                    .addAsResource("jwtPublicKey.pem")
                    .addAsResource("jwtPrivateKey.pem")
                    .addAsResource("applicationJwtSign.properties", "application.properties"));

    @Test
    public void verify() throws Exception {
        String token1 = generateToken("alice", "user");
        RestAssured.given().auth()
                .oauth2(token1)
                .when().get("/endp/claims")
                .then()
                .statusCode(200).body(equalTo("alice:user:" + token1));
        String token2 = generateToken("bob", "admin");
        RestAssured.given().auth()
                .oauth2(token2)
                .when().get("/endp/claims")
                .then()
                .statusCode(200).body(equalTo("bob:admin:" + token2));
    }

    private String generateToken(String upn, String role) {
        return Jwt.upn(upn).issuer("immo-jwt").groups(role).sign();
    }
}
