package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.jwt.build.Jwt;

public class DefaultGroupsSignEncryptUnitTest {
    private static Class<?>[] testClasses = {
            DefaultGroupsEndpoint.class,
            TokenUtils.class
    };
    /**
     * The test generated JWT token string
     */
    private String token;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem")
                    .addAsResource("applicationDefaultGroupsSignEncrypt.properties", "application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        token = Jwt.issuer("https://server.example.com").upn("upn").innerSign()
                .keyAlgorithm(KeyEncryptionAlgorithm.RSA_OAEP)
                .encrypt();
    }

    /**
     * Validate a request with MP-JWT without a 'groups' claim is successful
     * due to the default value being provided in the configuration
     *
     */
    @Test
    public void echoGroups() {
        RestAssured.given().auth()
                .oauth2(token)
                .get("/endp/echo")
                .then().assertThat().statusCode(200)
                .body(equalTo("User"));
    }
}
