package io.quarkus.jwt.test;

import java.net.HttpURLConnection;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DefaultGroupsUnitTest {
    private static Class<?>[] testClasses = {
            DefaultGroupsEndpoint.class
    };
    /**
     * The test generated JWT token string
     */
    private String token;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("applicationDefaultGroups.properties", "application.properties"));

    @BeforeEach
    public void generateToken() throws Exception {
        token = TokenUtils.generateTokenString("/TokenNoGroups.json");
    }

    /**
     * Validate a request with MP-JWT without a 'groups' claim is successful
     * due to the default value being provided in the configuration
     *
     * @throws Exception
     */
    @Test
    public void echoGroups() throws Exception {
        io.restassured.response.Response response = RestAssured.given().auth()
                .oauth2(token)
                .get("/endp/echo").andReturn();

        Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        String replyString = response.body().asString();
        // The missing 'groups' claim's default value, 'User' is expected
        Assertions.assertEquals("User", replyString);
    }
}
