package io.quarkus.smallrye.openapi.test.vertx;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;

public class OpenApiHttpRootPathCorsTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(OpenApiRoute.class)
                    .addAsResource(new StringAsset("quarkus.http.cors.enabled=true\n" +
                            "quarkus.http.cors.origins=*\n" +
                            "quarkus.http.non-application-root-path=/api/q\n" +
                            "quarkus.http.root-path=/api"), "application.properties"));

    @Test
    public void testCorsFilterProperties() {
        // make sure CORS are present when path is attached to main router and CORS are enabled
        RestAssured
                .given()
                .header("Origin", "https://quarkus.io")
                .log().all().filter(new ResponseLoggingFilter())
                .get("/q/openapi")
                .then()
                .statusCode(200)
                .header("access-control-allow-origin", "https://quarkus.io")
                .header("access-control-allow-credentials", "false");
    }
}
