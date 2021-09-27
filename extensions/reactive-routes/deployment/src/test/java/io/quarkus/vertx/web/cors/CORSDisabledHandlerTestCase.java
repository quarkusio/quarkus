package io.quarkus.vertx.web.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsNull.nullValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.TestRoute;

public class CORSDisabledHandlerTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestRoute.class));

    @Test
    @DisplayName("Doesn't return CORS headers if not configured")
    public void corsPreflightTest() {
        String origin = "http://custom.origin.quarkus";
        String methods = "GET,POST";
        String headers = "X-Custom";
        given().header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", nullValue())
                .header("Access-Control-Allow-Methods", nullValue())
                .header("Access-Control-Allow-Headers", nullValue());
    }
}
