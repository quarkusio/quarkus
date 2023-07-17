package io.quarkus.resteasy.reactive.server.test.security;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class LazyAuthRolesAllowedConfigExpTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RolesAllowedResource.class, UserResource.class,
                            TestIdentityProvider.class,
                            TestIdentityController.class,
                            SecurityOverrideFilter.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n" +
                            "admin-config-property=admin\n"), "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testRolesAllowedConfigExp() {
        RestAssured.given()
                .header("user", "admin")
                .header("role", "admin")
                .get("/roles/admin-config-exp")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("admin-config-exp"));
    }

}
