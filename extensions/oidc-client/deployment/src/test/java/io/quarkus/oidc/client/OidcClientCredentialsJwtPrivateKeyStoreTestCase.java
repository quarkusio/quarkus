package io.quarkus.oidc.client;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmClientCredentialsJwtPrivateKeyStoreManager.class)
public class OidcClientCredentialsJwtPrivateKeyStoreTestCase {

    private static Class<?>[] testClasses = {
            OidcClientResource.class,
            ProtectedResource.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-credentials-jwt-private-key-store.properties",
                            "application.properties")
                    .addAsResource("exportedCertificate.pem")
                    .addAsResource("exportedPrivateKey.pem")
                    .addAsResource("keystore.jks"));

    @Test
    public void testClientCredentialsToken() {
        String token = RestAssured.when().get("/client/token").body().asString();
        RestAssured.given().auth().oauth2(token)
                .when().get("/protected")
                .then()
                .statusCode(200)
                .body(equalTo("service-account-quarkus-app"));
    }
}
