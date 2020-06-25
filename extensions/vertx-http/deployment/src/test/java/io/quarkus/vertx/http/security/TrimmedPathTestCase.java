package io.quarkus.vertx.http.security;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class TrimmedPathTestCase {

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    private static final String APP_PROPS = "" +
            "# Add your application.properties here, if applicable.\n" +
            "quarkus.http.auth.permission.authenticated.paths=/*\n" +
            "quarkus.http.auth.permission.authenticated.policy=authenticated\n" +
            "#allow /health/* always for probeness\n" +
            "quarkus.http.auth.permission.health.paths=/health/*                            \n" + //note the spaces
            "quarkus.http.auth.permission.health.policy=permit\n" +
            "quarkus.http.auth.permission.health.methods=GET\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testHealthAccessible() {

        RestAssured
                .given()
                .when()
                .get("/health/liveliness")
                .then()
                .assertThat()
                .statusCode(200);
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/health/liveliness")
                .then()
                .assertThat()
                .statusCode(200);

        RestAssured
                .given()
                .when()
                .get("/foo")
                .then()
                .assertThat()
                .statusCode(401);
    }

}
