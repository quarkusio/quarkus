package io.quarkus.vertx.http.management;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import java.net.URL;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.ManagementInterface;

public class ManagementCorsTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest().setArchiveProducer(new Supplier<>() {
        @Override
        public JavaArchive get() {
            return ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ManagementPathHandler.class)
                    .addAsResource(new StringAsset("""
                            quarkus.management.enabled=true
                            quarkus.management.cors.enabled=true
                            quarkus.management.cors.origins=http://allowed.origin
                            """), "application.properties");
        }
    });

    @TestHTTPResource(value = "/cors-test", management = true)
    URL corsTest;

    @Test
    public void testUnexpectedOriginReturns403() {
        given().header("Origin", "http://unexpected.origin")
                .when()
                .get(corsTest)
                .then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    public void testAllowedOriginReturns200() {
        given().header("Origin", "http://allowed.origin")
                .when()
                .get(corsTest)
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://allowed.origin");
    }

    @Test
    public void testSameOriginWithoutExplicitConfigReturns200() {
        String sameOrigin = corsTest.getProtocol() + "://" + corsTest.getHost() + ":" + corsTest.getPort();
        given().header("Origin", sameOrigin)
                .when()
                .get(corsTest)
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", sameOrigin);
    }

    public static class ManagementPathHandler {
        void setup(@Observes ManagementInterface mi) {
            mi.router().get("/q/cors-test").handler(event -> {
                event.response().end("management cors test");
            });
        }
    }
}
