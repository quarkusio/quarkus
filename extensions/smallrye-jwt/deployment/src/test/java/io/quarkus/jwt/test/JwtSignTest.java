package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

public class JwtSignTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClass(JwtSignEndpoint.class).addAsResource("jwtPublicKey.pem").addAsResource("jwtPrivateKey.pem")
            .addAsResource("applicationJwtSign.properties", "application.properties"));

    @Test
    public void testAccessAllowed() {
        Set<String> roles = new HashSet<>(List.of("admin"));
        long duration = System.currentTimeMillis() + 3600;
        String jwt = Jwt.issuer("immo-jwt").subject("immo-jwt").groups(roles).expiresAt(duration).sign();

        RestAssured.given().auth().oauth2(jwt).get("/jwtsign").then().assertThat().statusCode(200)
                .body(equalTo("success"));
    }

    @Test
    public void testAccessDenied() {
        Set<String> roles = new HashSet<>(List.of("user"));
        long duration = System.currentTimeMillis() + 3600;
        String jwt = Jwt.issuer("immo-jwt").subject("immo-jwt").groups(roles).expiresAt(duration).sign();

        RestAssured.given().auth().oauth2(jwt).get("/jwtsign").then().assertThat().statusCode(403);
    }

    @Test
    public void testBadToken() {
        RestAssured.given().auth().oauth2("123").get("/jwtsign").then().assertThat().statusCode(401);
    }
}
