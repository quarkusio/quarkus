package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HostValidationAllowedHostsTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.host-validation.allowed-hosts=public-api.com,another-public-api.com,moRe-puBlic-aPi.com
                            quarkus.log.category."io.quarkus.vertx.http.runtime.HostValidationFilter".level=DEBUG
                            """), "application.properties"))
            .setLogRecordPredicate(r -> true)
            .assertLogRecords(r -> assertLogRecord(r));

    @Test
    void localHostRequest() {
        given().header("Host", "localhost:8081")
                .get("/test").then()
                .statusCode(403)
                .body(emptyString());
    }

    @Test
    void localHostloopbackRequest() {
        given().header("Host", "127.0.0.1:8081")
                .get("/test").then()
                .statusCode(403)
                .body(emptyString());
    }

    @Test
    void invalidLongHostRequest() {
        given().header("Host", "www.verylonghost-name-myservice-mymicroservice-localhost.com")
                .get("/test").then()
                .statusCode(403)
                .body(emptyString());
    }

    @Test
    void publicHostRequest() {
        given().header("Host", "public-api.com:8080")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void publicHostCaseInsensitiveRequest() {
        given().header("Host", "Public-Api.com:8080")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void anotherPublicHostRequest() {
        given().header("Host", "another-public-api.com:8080")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void anotherPublicHostCaseInsensitiveRequest() {
        given().header("Host", "Another-Public-Api.com:8080")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void morePublicHostCaseInsensitiveRequest() {
        given().header("Host", "More-Public-Api.com:8080")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    static class LocalBeanRegisteringRoute {

        void init(@Observes Router router) {
            Handler<RoutingContext> handler = rc -> rc.response().end("test route");

            router.get("/test").handler(handler);
        }
    }

    private static void assertLogRecord(List<LogRecord> records) {
        List<LogRecord> invalidHostRecords = records.stream()
                .filter(r -> r.getMessage().endsWith("rejected by the Host Validation filter")).collect(Collectors.toList());
        assertEquals(3, invalidHostRecords.size());
        List<LogRecord> invalidLocalHostRecords = invalidHostRecords.stream()
                .filter(r -> r.getMessage().contains("Request host localhost")).collect(Collectors.toList());
        assertEquals(1, invalidLocalHostRecords.size());
        List<LogRecord> invalidLoopbackRecords = invalidHostRecords.stream()
                .filter(r -> r.getMessage().contains("Request host 127.0.0.1")).collect(Collectors.toList());
        assertEquals(1, invalidLoopbackRecords.size());
        List<LogRecord> invalidLongHostRecords = invalidHostRecords.stream()
                .filter(r -> r.getMessage().contains("Request host " +
                        "www.verylonghost-name-myservice-mymicroservice-localhost.com".substring(0, 30) + "..."))
                .collect(Collectors.toList());
        assertEquals(1, invalidLongHostRecords.size());
    }
}
