package io.quarkus.oidc.client;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmClientCredentialsJwtPrivateKeyStoreManager.class)
public class OidcClientCredentialsJwtPrivateP12KeyStoreTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OidcClientResource.class, ProtectedResource.class)
                    .addAsResource("application-oidc-client-credentials-jwt-private-p12-key-store.properties",
                            "application.properties")
                    .addAsResource("exportedCertificate.pem")
                    .addAsResource("exportedPrivateKey.pem")
                    .addAsResource("keystore.pkcs12"));

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
