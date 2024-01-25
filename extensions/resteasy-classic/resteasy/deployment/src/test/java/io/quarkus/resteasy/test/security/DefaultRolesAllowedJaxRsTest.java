package io.quarkus.resteasy.test.security;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

public class DefaultRolesAllowedJaxRsTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PermitAllResource.class, UnsecuredResource.class,
                            TestIdentityProvider.class, UnsecuredResourceInterface.class,
                            TestIdentityController.class, UnsecuredParentResource.class,
                            UnsecuredSubResource.class, HelloResource.class)
                    .addAsResource(new StringAsset("quarkus.security.jaxrs.default-roles-allowed = admin\n"),
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
        assertStatus(path, 200, 403, 401);
    }

    @Test
    public void shouldDenyUnannotatedOnParentClass() {
        String path = "/unsecured/defaultSecurityParent";
        assertStatus(path, 200, 403, 401);
    }

    @Test
    public void shouldDenyUnannotatedOnInterface() {
        String path = "/unsecured/defaultSecurityInterface";
        assertStatus(path, 200, 403, 401);
    }

    @Test
    public void shouldDenyDenyAllMethod() {
        String path = "/unsecured/denyAll";
        assertStatus(path, 403, 403, 401);
    }

    @Test
    public void shouldPermitPermitAllMethod() {
        assertStatus("/unsecured/permitAll", 200, 200, 200);
    }

    @Test
    public void shouldDenySubResource() {
        String path = "/unsecured/sub/subMethod";
        assertStatus(path, 200, 403, 401);
    }

    @Test
    public void shouldAllowPermitAllSubResource() {
        String path = "/unsecured/permitAllSub/subMethod";
        assertStatus(path, 200, 200, 200);
    }

    @Test
    public void shouldAllowPermitAllClass() {
        String path = "/permitAll/sub/subMethod";
        assertStatus(path, 200, 200, 200);
    }

    @Test
    public void testNonEndpointMethodAreNotDenied() {
        // ensure io.quarkus.resteasy.test.security.DefaultRolesAllowedJaxRsTest.HelloResource.getHello is not secured with RolesAllowedInterceptor
        given().auth().preemptive()
                .basic("user", "user")
                .get("/hello")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"));
    }

    private void assertStatus(String path, int adminStatus, int userStatus, int anonStatus) {
        given().auth().preemptive()
                .basic("admin", "admin").get(path)
                .then()
                .statusCode(adminStatus);
        given().auth().preemptive()
                .basic("user", "user").get(path)
                .then()
                .statusCode(userStatus);
        when().get(path)
                .then()
                .statusCode(anonStatus);

    }

    @Path("/hello")
    public static class HelloResource {

        @RolesAllowed("**")
        @GET
        public String hello() {
            return getHello();
        }

        public String getHello() {
            return "hello";
        }

    }

}
