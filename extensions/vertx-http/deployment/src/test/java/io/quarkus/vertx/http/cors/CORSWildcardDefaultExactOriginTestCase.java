package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verifies the default behavior ({@code return-exact-origins=true}) where the CORS
 * filter echoes back the request origin even when {@code origins=*} is configured.
 */
public class CORSWildcardDefaultExactOriginTestCase {

    private static final String APP_PROPS = "" +
            "quarkus.http.cors.enabled=true\n" +
            "quarkus.http.cors.origins=*\n" +
            "quarkus.http.cors.methods=GET,POST,OPTIONS\n";
    // return-exact-origins defaults to true

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanRegisteringRoute.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties");
        }
    });

    @Test
    @DisplayName("Default behavior echoes request origin for simple request")
    public void corsDefaultEchoesOrigin() {
        String origin = "http://custom.origin.quarkus";
        given().header("Origin", origin)
                .when()
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", is(origin))
                .body(is("test route"));
    }

    @Test
    @DisplayName("Default behavior echoes request origin for preflight request")
    public void corsDefaultPreflightEchoesOrigin() {
        String origin = "http://custom.origin.quarkus";
        given().header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", is(origin));
    }
}
