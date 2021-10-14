package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DefaultGroupsCustomFactoryUnitTest {
    private static Class<?>[] testClasses = {
            DefaultGroupsEndpoint.class,
            TestJWTCallerPrincipalFactory.class,
            TokenUtils.class
    };
    /**
     * The test generated JWT token string
     */
    private String token;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("privateKey.pem")
                    .addAsResource("TokenUserGroup.json")
                    .addAsResource("applicationCustomFactory.properties", "application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        token = TokenUtils.generateTokenString("/TokenUserGroup.json");
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

    @Test
    public void echoGroupsWithParser() {
        RestAssured.given().auth()
                .oauth2(token)
                .get("/endp/echo-parser")
                .then().assertThat().statusCode(200)
                .body(equalTo("parser:User"));
    }
}
