package io.quarkus.resteasy.reactive.server.test.security;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.security.test.utils.TestIdentityController;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

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
    public void testStringPermission2RequiredPermissionsNonBlocking_MetaAnnotation() {
        // invokes POST /permissions-non-blocking endpoint that requires 2 permissions: create AND update
        // meta annotation is used

        // admin must have both 'create' and 'update' in order to succeed
        RestAssured.given().auth().basic("admin", "admin").post("/permissions-non-blocking/meta-create-update")
                .then().statusCode(200).body(Matchers.equalTo("done"));

        // user has only 'update', therefore should fail
        RestAssured.given().auth().basic("user", "user").post("/permissions-non-blocking/meta-create-update")
                .then().statusCode(403);
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

    @Test
    public void testCustomPermissionWithAdditionalArgs() {
        // === autodetected method params && non-blocking endpoint
        // admin has permission with place 'Ostrava'
        reqAutodetectedExtraArgs("admin", "Ostrava")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Ostrava"));
        // user has permission with place 'Prague'
        reqAutodetectedExtraArgs("user", "Prague")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Prague"));
        reqAutodetectedExtraArgs("user", "Ostrava")
                .statusCode(403);
        // viewer has no permission
        reqAutodetectedExtraArgs("viewer", "Ostrava")
                .statusCode(403);

        // === explicitly marked method params && blocking endpoint
        // admin has permission with place 'Ostrava'
        reqExplicitlyMarkedExtraArgs("admin", "Ostrava")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Ostrava"));
        // user has permission with place 'Prague'
        reqExplicitlyMarkedExtraArgs("user", "Prague")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Prague"));
        reqExplicitlyMarkedExtraArgs("user", "Ostrava")
                .statusCode(403);
        // viewer has no permission
        reqExplicitlyMarkedExtraArgs("viewer", "Ostrava")
                .statusCode(403);
    }

    @Test
    public void testCustomPermissionWithAdditionalArgs_MetaAnnotation() {
        // === explicitly marked method params && blocking endpoint
        // admin has permission with place 'Ostrava'
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("admin", "Ostrava")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Ostrava"));
        // user has permission with place 'Prague'
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("user", "Prague")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Prague"));
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("user", "Ostrava")
                .statusCode(403);
        // viewer has no permission
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("viewer", "Ostrava")
                .statusCode(403);
    }

    private static ValidatableResponse reqAutodetectedExtraArgs(String user, String place) {
        return RestAssured.given()
                .auth().basic(user, user)
                .pathParam("goodbye", "so long")
                .header("toWhom", "Nelson")
                .cookie("day", 3)
                .body(place)
                .post("/permissions-non-blocking/custom-perm-with-args/{goodbye}").then();
    }

    private static ValidatableResponse reqExplicitlyMarkedExtraArgs(String user, String place) {
        return RestAssured.given()
                .auth().basic(user, user)
                .pathParam("goodbye", "so long")
                .header("toWhom", "Nelson")
                .cookie("day", 3)
                .body(place)
                .post("/permissions/custom-perm-with-args/{goodbye}").then();
    }

    private static ValidatableResponse reqExplicitlyMarkedExtraArgs_MetaAnnotation(String user, String place) {
        return RestAssured.given()
                .auth().basic(user, user)
                .pathParam("goodbye", "so long")
                .header("toWhom", "Nelson")
                .cookie("day", 3)
                .body(place)
                .post("/permissions/custom-perm-with-args-meta-annotation/{goodbye}").then();
    }
}
