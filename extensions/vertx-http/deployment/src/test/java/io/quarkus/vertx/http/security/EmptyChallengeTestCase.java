package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EmptyChallengeTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.auth.permission.roles1.paths=/*\n" +
            "quarkus.http.auth.permission.roles1.policy=authenticated\n";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<JavaArchive>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HeaderAuthenticator.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    public void testNoChallenge() {

        RestAssured
                .given()
                .header("user", "test")
                .when()
                .get("/path")
                .then()
                .assertThat()
                .statusCode(200)
                .body(equalTo("test:/path"));
        RestAssured
                .given()
                .when()
                .get("/path")
                .then()
                .assertThat()
                .statusCode(401);

    }
}
