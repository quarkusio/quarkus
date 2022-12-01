package io.quarkus.oidc.client;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmClientCredentialsJwtSecretManager.class)
public class OidcClientCredentialsJwtSecretTestCase {

    private static Class<?>[] testClasses = {
            OidcClientsResource.class,
            ProtectedResource.class,
            SecretProvider.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-credentials-jwt-secret.properties", "application.properties"));

    @Test
    public void testGetTokenJwtClient() {
        String token = RestAssured.when().get("/clients/token/jwt").body().asString();
        RestAssured.given().auth().oauth2(token)
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));
    }

    @Test
    public void testGetTokensJwtClient() {
        String[] tokens = RestAssured.when().get("/clients/tokens/jwt").body().asString().split(" ");
        assertTokensNotNull(tokens);

        RestAssured.given().auth().oauth2(tokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));
    }

    private static void assertTokensNotNull(String[] tokens) {
        assertEquals(2, tokens.length);
        assertNotNull(tokens[0]);
        assertEquals("null", tokens[1]);
    }
}
