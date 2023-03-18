package io.quarkus.resteasy.reactive.server.test.security;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.security.test.utils.TestIdentityController;
import io.restassured.RestAssured;

public abstract class AbstractPermissionsAllowedTestCase {

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin")
                .add("user", "user")
                .add("viewer", "viewer");
    }

    @Test
    public void testStringPermission2RequiredPermissions() {
        // invokes POST /permissions endpoint that requires 2 permissions: create AND update

        // admin must have both 'create' and 'update' in order to succeed
        RestAssured.given().auth().basic("admin", "admin").post("/permissions").then().statusCode(200)
                .body(Matchers.equalTo("done"));

        // user has only 'update', therefore should fail
        RestAssured.given().auth().basic("user", "user").post("/permissions").then().statusCode(403);
    }

    @Test
    public void testStringPermission2RequiredPermissionsNonBlocking() {
        // invokes POST /permissions-non-blocking endpoint that requires 2 permissions: create AND update

        // admin must have both 'create' and 'update' in order to succeed
        RestAssured.given().auth().basic("admin", "admin").post("/permissions-non-blocking").then().statusCode(200)
                .body(Matchers.equalTo("done"));

        // user has only 'update', therefore should fail
        RestAssured.given().auth().basic("user", "user").post("/permissions-non-blocking").then().statusCode(403);
    }

    @Test
    public void testStringPermissionOneOfPermissionsAndActions() {
        // invokes GET /permissions/admin endpoint that requires one of 2 permissions: read:resource-admin, read:resource-user

        // admin has 'read:resource-admin', therefore succeeds
        RestAssured.given().auth().basic("admin", "admin").get("/permissions/admin").then().statusCode(200)
                .body(Matchers.equalTo("admin"));

        // user has 'read:resource-user', therefore succeeds
        RestAssured.given().auth().basic("user", "user").get("/permissions/admin").then().statusCode(200)
                .body(Matchers.equalTo("admin"));

        // viewer has 'read:resource-viewer', therefore fails
        RestAssured.given().auth().basic("viewer", "viewer").get("/permissions/admin").then().statusCode(403);
    }

    @Test
    public void testStringPermissionOneOfPermissionsAndActionsNonBlocking() {
        // invokes GET /permissions-non-blocking/admin endpoint that requires one of 2 permissions: read:resource-admin, read:resource-user

        // admin has 'read:resource-admin', therefore succeeds
        RestAssured.given().auth().basic("admin", "admin").get("/permissions-non-blocking/admin").then().statusCode(200)
                .body(Matchers.equalTo("admin"));

        // user has 'read:resource-user', therefore succeeds
        RestAssured.given().auth().basic("user", "user").get("/permissions-non-blocking/admin").then().statusCode(200)
                .body(Matchers.equalTo("admin"));

        // viewer has 'read:resource-viewer', therefore fails
        RestAssured.given().auth().basic("viewer", "viewer").get("/permissions-non-blocking/admin").then().statusCode(403);
    }

    @Test
    public void testBlockingAccessToIdentityOnIOThread() {
        // invokes GET /permissions/security-identity endpoint that requires one permission: get-identity

        // - blocking path

        // user has 'get-identity, therefore succeeds
        RestAssured.given().auth().basic("user", "user").get("/permissions/admin/security-identity").then().statusCode(200)
                .body(Matchers.equalTo("user"));

        // admin lack 'get-identity', therefore fails
        RestAssured.given().auth().basic("admin", "admin").get("/permissions/admin/security-identity").then().statusCode(403);

        // - non-blocking path

        // user has 'get-identity, therefore succeeds
        RestAssured.given().auth().basic("user", "user").get("/permissions-non-blocking/admin/security-identity").then()
                .statusCode(200)
                .body(Matchers.equalTo("user"));

        // admin lack 'get-identity', therefore fails
        RestAssured.given().auth().basic("admin", "admin").get("/permissions-non-blocking/admin/security-identity").then()
                .statusCode(403);
    }

    @Test
    public void testCustomPermissionNonBlocking() {
        // invokes GET /permissions/custom-permission endpoint that requires query param 'hello'

        // we send 'hello' => pass
        RestAssured.given().auth().basic("admin", "admin").param("greeting", "hello")
                .get("/permissions-non-blocking/custom-permission").then().statusCode(200).body(Matchers.equalTo("hello"));

        // we send 'hi' => fail
        RestAssured.given().auth().basic("admin", "admin").param("greeting", "hi")
                .get("/permissions-non-blocking/custom-permission").then().statusCode(403);
    }

    @Test
    public void testCustomPermission() {
        // invokes GET /permissions/custom-permission endpoint that requires query param 'hello'

        // we send 'hello' => pass
        RestAssured.given().auth().basic("admin", "admin").param("greeting", "hello")
                .get("/permissions/custom-permission").then().statusCode(200).body(Matchers.equalTo("hello"));

        // we send 'hi' => pass
        RestAssured.given().auth().basic("admin", "admin").param("greeting", "hi")
                .get("/permissions/custom-permission").then().statusCode(403);
    }
}
