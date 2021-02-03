package io.quarkus.oidc.client;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

/**
 * TODO: Vert.x 4 Migration: need non-Vert.x OIDC impl
 */
@Disabled
@QuarkusTestResource(KeycloakRealmClientCredentialsJwtPrivateKeyManager.class)
public class OidcClientCredentialsJwtPrivateKeyTestCase {

    private static Class<?>[] testClasses = {
            OidcClientResource.class,
            ProtectedResource.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-credentials-jwt-private-key.properties", "application.properties")
                    .addAsResource("publicKey.pem")
                    .addAsResource("privateKey.pem"));

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
