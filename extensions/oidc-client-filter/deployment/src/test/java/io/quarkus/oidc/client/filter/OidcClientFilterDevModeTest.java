package io.quarkus.oidc.client.filter;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;

@QuarkusTestResource(KeycloakRealmResourceManager.class)
public class OidcClientFilterDevModeTest {

    private static Class<?>[] testClasses = {
            FrontendResource.class,
            ProtectedResource.class,
            ProtectedResourceService.class
    };

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(testClasses)
                    .addAsResource("application-oidc-client-filter.properties", "application.properties"));

    /**
     * TODO: Vert.x 4 Migration: need non-Vert.x OIDC impl
     */
    @Test
    @Disabled
    public void testGetUserName() {
        RestAssured.when().get("/frontend/user-before-registering-provider")
                .then()
                .statusCode(401)
                .body(equalTo("ProtectedResourceService requires a token"));
        test.modifyResourceFile("application.properties", s -> s.replace("#", ""));
        RestAssured.when().get("/frontend/user-after-registering-provider")
                .then()
                .statusCode(200)
                .body(equalTo("alice"));
    }

}
