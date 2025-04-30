package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

public class RepeatedPermissionsAllowedTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, HelloResource.class)
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.log.category.\"io.quarkus.vertx.http.runtime.QuarkusErrorHandler\".level=OFF"
                                            + System.lineSeparator()),
                            "application.properties"));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("user", "user", new StringPermission("read"))
                .add("admin", "admin", new StringPermission("read"), new StringPermission("write"));
    }

    @Test
    public void testRepeatedPermissionsAllowedOnClass() {
        // anonymous user
        RestAssured.given()
                .body("{%$$#!#@") // assures checks are eager
                .post("/hello")
                .then()
                .statusCode(401);
        // authenticated user, insufficient rights
        RestAssured.given()
                .auth().preemptive().basic("user", "user")
                .body("{%$$#!#@") // assures checks are eager
                .post("/hello")
                .then()
                .statusCode(403);
        // authorized user, invalid payload
        RestAssured.given()
                .auth().preemptive().basic("admin", "admin")
                .body("{%$$#!#@") // assures checks are eager
                .post("/hello")
                .then()
                .statusCode(500);
    }

    @Test
    public void testRepeatedPermissionsAllowedOnInterface() {
        // anonymous user
        RestAssured.given()
                .body("{%$$#!#@") // assures checks are eager
                .post("/hello-interface")
                .then()
                .statusCode(401);
        // authenticated user, insufficient rights
        RestAssured.given()
                .auth().preemptive().basic("user", "user")
                .body("{%$$#!#@") // assures checks are eager
                .post("/hello-interface")
                .then()
                .statusCode(403);
        // authorized user, invalid payload
        RestAssured.given()
                .auth().preemptive().basic("admin", "admin")
                .body("{%$$#!#@") // assures checks are eager
                .post("/hello-interface")
                .then()
                .statusCode(500);
    }

    @Path("/hello")
    public static class HelloResource {

        @PermissionsAllowed(value = "write")
        @PermissionsAllowed(value = "read")
        @POST
        public String sayHello(JsonObject entity) {
            return "ignored";
        }
    }

    @Path("/hello-interface")
    public interface HelloInterface {

        @PermissionsAllowed(value = "write")
        @PermissionsAllowed(value = "read")
        @POST
        String sayHello(JsonObject entity);
    }

    public static class HelloInterfaceImpl implements HelloInterface {

        @Override
        public String sayHello(JsonObject entity) {
            return "ignored";
        }
    }
}
