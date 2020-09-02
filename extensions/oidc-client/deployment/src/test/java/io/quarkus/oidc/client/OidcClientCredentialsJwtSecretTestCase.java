package io.quarkus.oidc.client;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTestResource(KeycloakRealmClientCredentialsJwtSecretManager.class)
public class OidcClientCredentialsJwtSecretTestCase {

    private static Class<?>[] testClasses = {
            OidcClientsResource.class,
            ProtectedResource.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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

    @Test
    public void testRefreshTokensJwtClient() {
        String[] tokens = RestAssured.when().get("/clients/tokens/jwt").body().asString().split(" ");
        assertTokensNotNull(tokens);

        RestAssured.given().auth().oauth2(tokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));

        // Wait until the access token has expired
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(1))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Response r = RestAssured.given().auth().oauth2(tokens[0])
                                .when().get("/protected").andReturn();
                        return r.getStatusCode() == 401;
                    }
                });

        String[] refreshedTokens = RestAssured.given().queryParam("refreshToken", tokens[1])
                .when().get("/clients/refresh-tokens/jwt").body().asString().split(" ");
        assertTokensNotNull(refreshedTokens);
        assertNotEquals(tokens[0], refreshedTokens[0]);

        RestAssured.given().auth().oauth2(refreshedTokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));
    }

    private static void assertTokensNotNull(String[] tokens) {
        assertEquals(2, tokens.length);
        assertNotNull(tokens[0]);
        assertNotNull(tokens[1]);
    }
}
