package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import java.security.PrivateKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

public class JwtParserUnitTest {
    private static Class<?>[] testClasses = {
            JwtParserEndpoint.class
    };

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("applicationJwtParser.properties", "application.properties"));

    @Test
    public void verifyTokenWithoutIssuedAt() throws Exception {
        RestAssured.given().auth()
                .oauth2(generateTokenWithoutIssuedAt())
                .get("/parser/name")
                .then().assertThat().statusCode(200)
                .body(equalTo("alice"));
    }

    @Test
    public void verifyTokenWithoutIssuedAtWithKey() throws Exception {
        RestAssured.given().auth()
                .oauth2(generateTokenWithoutIssuedAt())
                .get("/parser/name-with-key")
                .then().assertThat().statusCode(200)
                .body(equalTo("alice"));
    }

    private String generateTokenWithoutIssuedAt() throws Exception {
        // 'smallrye.jwt.new-token.add-default-claims' is set to 'false' so that no 'iat' claim is added
        PrivateKey privateKey = KeyUtils.readPrivateKey("privateKey.pem");
        return Jwt.subject("alice")
                .issuer("https://server.example.com")
                .expiresAt(System.currentTimeMillis() / 1000 + 5)
                .sign(privateKey);
    }
}
