package io.quarkus.vertx.http.security;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
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

public class CustomNamedHttpSecPolicyTest {

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
    }

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.permission.authenticated.paths=admin\n" +
            "quarkus.http.auth.permission.authenticated.policy=custom123\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, AdminPathHandler.class,
                            CustomNamedHttpSecPolicy.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testAdminPath() {
        RestAssured
                .given()
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(401);
        RestAssured
                .given()
                .when()
                .header("hush-hush", "ignored")
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(Matchers.equalTo(":/admin"));
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .header("hush-hush", "ignored")
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(200)
                .body(Matchers.equalTo("test:/admin"));
        RestAssured
                .given()
                .auth()
                .preemptive()
                .basic("test", "test")
                .when()
                .get("/admin")
                .then()
                .assertThat()
                .statusCode(403);
    }

}
