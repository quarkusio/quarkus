package io.quarkus.oidc.token.propagation.deployment.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.client.OidcTestClient;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class OidcTokenPropagationTest {

    final static OidcTestClient client = new OidcTestClient();

    private static Class<?>[] testClasses = {
            FrontendResource.class,
            ProtectedResource.class,
            AccessTokenPropagationService.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application.properties"));

    @AfterAll
    public static void close() {
        client.close();
    }

    @Test
    public void testGetUserNameWithTokenPropagation() {
        RestAssured.given().auth().oauth2(getBearerAccessToken())
                .when().get("/frontend/token-propagation")
                .then()
                .statusCode(200)
                .body(equalTo("Token issued to alice has been exchanged, new user name: bob"));
    }

    public String getBearerAccessToken() {
        return client.getAccessToken("alice", "alice");
    }

}
