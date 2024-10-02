package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

public class SignMultipleAlgorithmsUnitTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultGroupsEndpoint.class)
                    .addAsResource("rs256PrivateKey.jwk")
                    .addAsResource("edEcPrivateKey.jwk")
                    .addAsResource("signatureJwkSet.jwk")
                    .addAsResource("applicationSignMultipleAlgorithms.properties", "application.properties"));

    @Test
    public void echoGroupsRS256() {
        String token = Jwt.issuer("https://server.example.com").upn("alice").groups("User")
                .sign("/rs256PrivateKey.jwk");
        RestAssured.given().auth()
                .oauth2(token)
                .get("/endp/echo")
                .then().assertThat().statusCode(200)
                .body(equalTo("User"));
    }

    @Test
    public void echoGroupsEdDSA() {
        String token = Jwt.issuer("https://server.example.com").upn("alice").groups("User")
                .sign("/edEcPrivateKey.jwk");
        RestAssured.given().auth()
                .oauth2(token)
                .get("/endp/echo")
                .then().assertThat().statusCode(200)
                .body(equalTo("User"));
    }
}
