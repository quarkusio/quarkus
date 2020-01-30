package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DefaultDenyTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.basic=true\n" +
            "quarkus.http.auth.permission.default-deny.paths=/*\n" +
            "quarkus.http.auth.permission.default-deny.policy=deny\n" +
            "quarkus.http.auth.permission.permit1.paths=/permit,/combined\n" +
            "quarkus.http.auth.permission.permit1.policy=permit\n" +
            "quarkus.http.auth.permission.permit2.paths=/permit-get\n" +
            "quarkus.http.auth.permission.permit2.methods=GET\n" +
            "quarkus.http.auth.permission.permit2.policy=permit\n" +
            "quarkus.http.auth.permission.deny1.paths=/deny,/combined\n" +
            "quarkus.http.auth.permission.deny1.policy=deny\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    @Test()
    public void testUncoveredMethod() {

        RestAssured
                .given()
                .when()
                .get("/unmentioned")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/unmentioned")
                .then()
                .assertThat()
                .statusCode(403);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/unmentioned")
                .then()
                .assertThat()
                .statusCode(403);
    }

    @Test
    public void testPermitAll() {

        RestAssured
                .given()
                .when()
                .get("/permit")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo(":/permit"));

        RestAssured
                .given()
                .when()
                .post("/permit")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo(":/permit"));

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/permit")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo(":/permit"));

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/permit")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/permit"));
    }

    @Test
    public void testPermitAllGetMethod() {

        RestAssured
                .given()
                .when()
                .get("/permit-get")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo(":/permit-get"));

        RestAssured
                .given()
                .when()
                .post("/permit-get")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testDenyAllCombinedWithPermitAll() {

        RestAssured
                .given()
                .when()
                .get("/combined")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void testDenyAll() {

        RestAssured
                .given()
                .when()
                .get("/deny")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/deny")
                .then()
                .assertThat()
                .statusCode(403);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/deny")
                .then()
                .assertThat()
                .statusCode(403);
    }
}
