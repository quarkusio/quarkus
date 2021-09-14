package io.quarkus.oidc.client;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmClientCredentialsManager.class)
public class OidcClientCredentialsTestCase {

    private static Class<?>[] testClasses = {
            OidcClientsResource.class,
            ProtectedResource.class,
            SecretProvider.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-credentials.properties", "application.properties"));

    @Test
    public void testGetTokenDefaultClient() {
        doTestGetTokenClient("default");
    }

    @Test
    public void testGetTokensDefaultClient() {
        doTestGetTokensClient("default");
    }

    @Test
    public void testGetTokensOnDemand() {
        String[] tokens = RestAssured.when().get("/clients/tokenOnDemand").body().asString().split(" ");
        assertTokensNotNull(tokens);

        RestAssured.given().auth().oauth2(tokens[0])
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));
    }

    private void doTestGetTokenClient(String clientId) {
        String token = RestAssured.when().get("/clients/token/" + clientId).body().asString();
        RestAssured.given().auth().oauth2(token)
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));

    }

    private void doTestGetTokensClient(String clientId) {
        String[] tokens = RestAssured.when().get("/clients/tokens/" + clientId).body().asString().split(" ");
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
