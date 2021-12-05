package io.quarkus.resteasy.reactive.server.test.security;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DenyAllJaxRsTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PermitAllResource.class, UnsecuredResource.class,
                            TestIdentityProvider.class,
                            TestIdentityController.class,
                            UnsecuredSubResource.class)
                    .addAsResource(new StringAsset("quarkus.security.jaxrs.deny-unannotated-endpoints = true\n"),
                            "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void shouldDenyUnannotated() {
        String path = "/unsecured/defaultSecurity";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldDenyDenyAllMethod() {
        String path = "/unsecured/denyAll";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldPermitPermitAllMethod() {
        assertStatus("/unsecured/permitAll", 200, 200);
    }

    @Test
    public void shouldDenySubResource() {
        String path = "/unsecured/sub/subMethod";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldAllowPermitAllSubResource() {
        String path = "/unsecured/permitAllSub/subMethod";
        assertStatus(path, 200, 200);
    }

    @Test
    public void shouldAllowPermitAllClass() {
        String path = "/permitAll/sub/subMethod";
        assertStatus(path, 200, 200);
    }

    private void assertStatus(String path, int status, int anonStatus) {
        given().auth().preemptive()
                .basic("admin", "admin").get(path)
                .then()
                .statusCode(status);
        given().auth().preemptive()
                .basic("user", "user").get(path)
                .then()
                .statusCode(status);
        when().get(path)
                .then()
                .statusCode(anonStatus);

    }

}
