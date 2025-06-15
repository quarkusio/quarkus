package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import java.security.PrivateKey;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.util.KeyUtils;

public class JwtParserUnitTest {
    private static Class<?>[] testClasses = { JwtParserEndpoint.class };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addClasses(testClasses).addAsResource("publicKey.pem").addAsResource("privateKey.pem")
                    .addAsResource("applicationJwtParser.properties", "application.properties"));

    @Test
    public void verifyTokenWithoutIssuedAt() throws Exception {
        RestAssured.given().auth().oauth2(generateTokenWithoutIssuedAt()).get("/parser/name").then().assertThat()
                .statusCode(200).body(equalTo("alice"));
    }

    @Test
    public void verifyTokenWithoutIssuedAtWithKey() throws Exception {
        RestAssured.given().auth().oauth2(generateTokenWithoutIssuedAt()).get("/parser/name-with-key").then()
                .assertThat().statusCode(200).body(equalTo("alice"));
    }

    private String generateTokenWithoutIssuedAt() throws Exception {
        String payload = "{" + "\"sub\":\"alice\"," + "\"iss\":\"https://server.example.com\"," + "\"exp\":"
                + (System.currentTimeMillis() / 1000 + 5) + "," + "}";

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(payload);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        PrivateKey privateKey = KeyUtils.readPrivateKey("privateKey.pem");
        jws.setKey(privateKey);
        return jws.getCompactSerialization();
    }
}
