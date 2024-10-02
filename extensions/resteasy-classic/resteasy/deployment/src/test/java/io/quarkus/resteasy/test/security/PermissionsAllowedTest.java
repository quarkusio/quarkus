package io.quarkus.resteasy.test.security;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public class PermissionsAllowedTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PermissionsAllowedResource.class, TestIdentityProvider.class, TestIdentityController.class,
                            StringPermissionsAllowedMetaAnnotation.class, CustomPermissionWithExtraArgs.class,
                            CreateOrUpdate.class));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", new StringPermission("create"), new StringPermission("update"),
                        new CustomPermissionWithExtraArgs("farewell", "so long", "Nelson", 3, "Ostrava"))
                .add("user", "user", new StringPermission("create"),
                        new CustomPermissionWithExtraArgs("farewell", "so long", "Nelson", 3, "Prague"))
                .add("viewer", "viewer");
    }

    @Test
    public void testPermissionsAllowedMetaAnnotation_StringPermissions() {
        RestAssured.get("/permissions/string-meta-annotation").then().statusCode(401);
        RestAssured.given().auth().basic("user", "user").get("/permissions/string-meta-annotation").then().statusCode(403);
        RestAssured.given().auth().basic("admin", "admin").get("/permissions/string-meta-annotation").then().statusCode(200);
    }

    @Test
    public void testPermissionsAllowedMetaAnnotation_CustomPermissionsWithArgs() {
        // === explicitly marked method params && blocking endpoint
        // admin has permission with place 'Ostrava'
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("admin", "Ostrava")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Ostrava"));
        // user has permission with place 'Prague'
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("user", "Prague")
                .statusCode(200)
                .body(Matchers.equalTo("so long Nelson 3 Prague"));
        // user doesn't have permission with place 'Ostrava'
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("user", "Ostrava")
                .statusCode(403);
        // viewer has no permission
        reqExplicitlyMarkedExtraArgs_MetaAnnotation("viewer", "Ostrava")
                .statusCode(403);
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
