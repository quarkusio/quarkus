package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DisabledPermissionTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.basic=true\n" +
            "quarkus.http.auth.permission.test1.paths=/default\n" +
            "quarkus.http.auth.permission.test1.policy=authenticated\n" +

            "quarkus.http.auth.permission.test2.enabled=false\n" +
            "quarkus.http.auth.permission.test2.paths=/disabled\n" +
            "quarkus.http.auth.permission.test2.policy=deny\n" +

            "quarkus.http.auth.permission.test3.enabled=true\n" +
            "quarkus.http.auth.permission.test3.paths=/enabled\n" +
            "quarkus.http.auth.permission.test3.policy=permit\n" +

            "quarkus.http.auth.permission.test4.enabled=true\n" +
            "quarkus.http.auth.permission.test4.paths=/restricted\n" +
            "quarkus.http.auth.permission.test4.methods=GET\n" +
            "quarkus.http.auth.permission.test4.policy=authenticated\n" +

            "quarkus.http.auth.permission.test5.enabled=false\n" +
            "quarkus.http.auth.permission.test5.paths=/unrestricted\n" +
            "quarkus.http.auth.permission.test5.methods=GET\n" +
            "quarkus.http.auth.permission.test5.policy=authenticated\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(
                    TestIdentityController.class,
                    TestIdentityProvider.class,
                    PathHandler.class)
            .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    @Test
    public void testAnonymousDenied() {
        RestAssured
                .given()
                .when()
                .get("/default")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testAnonymousAllowed() {
        RestAssured
                .given()
                .when()
                .get("/enabled")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo(":/enabled"));
    }

    @Test
    public void testDefaultEnabled() {
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/default")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/default"));
    }

    @Test
    public void testDisabled() {
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/disabled")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/disabled"));
    }

    @Test
    public void testEnabled() {
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/enabled")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/enabled"));
    }

    @Test
    public void testRestricted() {
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .post("/restricted")
                .then()
                .assertThat()
                .statusCode(403);
    }

    @Test
    public void testUnrestricted() {
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/unrestricted")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/unrestricted"));
    }

}
