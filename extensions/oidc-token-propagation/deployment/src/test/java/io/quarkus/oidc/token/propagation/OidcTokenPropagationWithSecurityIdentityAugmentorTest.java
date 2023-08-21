package io.quarkus.oidc.token.propagation;

import static io.quarkus.oidc.token.propagation.RolesSecurityIdentityAugmentor.SUPPORTED_USER;
import static org.hamcrest.Matchers.equalTo;

import java.util.Set;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(OidcWiremockTestResource.class)
public class OidcTokenPropagationWithSecurityIdentityAugmentorTest {

    private static Class<?>[] testClasses = {
            FrontendResource.class,
            ProtectedResource.class,
            AccessTokenPropagationService.class,
            RolesResource.class,
            RolesService.class,
            RolesSecurityIdentityAugmentor.class
    };

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(testClasses)
                    .addAsResource("application.properties")
                    .addAsResource(new StringAsset("quarkus.oidc-token-propagation.enabled-during-authentication=true\n" +
                            "quarkus.rest-client.\"roles\".uri=http://localhost:8081/roles\n"),
                            "META-INF/microprofile-config.properties"));

    @Test
    public void testGetUserNameWithTokenPropagation() {
        // request only succeeds if SecurityIdentityAugmentor managed to acquire 'tester' role for user 'alice'
        // and that is only possible if access token is propagated during augmentation
        RestAssured.given().auth().oauth2(getBearerAccessToken())
                .when().get("/frontend/token-propagation-with-augmentor")
                .then()
                .statusCode(200)
                .body(equalTo("Token issued to alice has been exchanged, new user name: bob"));
    }

    public String getBearerAccessToken() {
        return OidcWiremockTestResource.getAccessToken(SUPPORTED_USER, Set.of("admin"));
    }

}
