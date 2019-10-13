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

public class RolesAllowedTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.basic=true\n" +
            "quarkus.http.auth.permission.roles1.paths=/roles1,/deny,/permit,/combined\n" +
            "quarkus.http.auth.permission.roles1.roles-allowed=test\n" +
            "quarkus.http.auth.permission.roles2.paths=/roles2,/deny,/permit/combined\n" +
            "quarkus.http.auth.permission.roles2.roles-allowed=admin\n" +
            "quarkus.http.auth.permission.permit1.paths=/permit\n" +
            "quarkus.http.auth.permission.permit1.permit-all=true\n" +
            "quarkus.http.auth.permission.deny1.paths=/deny,/combined\n" +
            "quarkus.http.auth.permission.deny1.deny-all=true\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class).addClasses(TestIdentityProvider.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    @Test
    public void testRolesAllowed() {

        RestAssured
                .given()
                .when()
                .get("/roles1")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/roles1")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/roles1"));

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/roles1")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/roles1"));
    }

    @Test
    public void testRolesAllowedWrongRoles() {

        RestAssured
                .given()
                .when()
                .get("/roles2")
                .then()
                .assertThat()
                .statusCode(401);

        RestAssured
                .given()
                .auth()
                .basic("test", "test")
                .when()
                .get("/roles2")
                .then()
                .assertThat()
                .statusCode(403);

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/roles2")
                .then()
                .assertThat()
                .statusCode(403);
    }

    @Test
    public void testRolesAllowedCombinedWithPermitAll() {

        RestAssured
                .given()
                .when()
                .get("/permit")
                .then()
                .assertThat()
                .statusCode(401);

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
    public void testRolesAllowedCombinedWithDenyAll() {

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

        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/combined")
                .then()
                .assertThat()
                .statusCode(403);
    }
}
