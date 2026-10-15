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

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HostValidationAllowedHostsTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset(
                            "quarkus.http.host-validation.allowed-hosts=public-api.com,another-public-api.com,moRe-puBlic-aPi.com\n"
                                    + "quarkus.log.category.\"io.quarkus.vertx.http.runtime.HostValidationFilter\".level=DEBUG\n"),
                            "application.properties"))
            .setLogRecordPredicate(r -> r.getLoggerName().contains("HostValidation"))
            .assertLogRecords(HostValidationAllowedHostsTest::assertLogRecords);

    @Test
    void localHostRequest() {
        given().header("Host", "localhost")
                .get("/test").then()
                .statusCode(400)
                .body(emptyString());
    }

    @Test
    void localHostRequestWithPort() {
        given().header("Host", "localhost:8081")
                .get("/test").then()
                .statusCode(400)
                .body(emptyString());
    }

    @Test
    void loopbackRequest() {
        given().header("Host", "127.0.0.1:8081")
                .get("/test").then()
                .statusCode(400)
                .body(emptyString());
    }

    @Test
    void invalidLongHostRequest() {
        given().header("Host", "www.verylonghost-name-myservice-mymicroservice-localhost.com")
                .get("/test").then()
                .statusCode(400)
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

    private static String formatMessage(LogRecord r) {
        if (r.getParameters() != null && r.getParameters().length > 0) {
            return String.format(r.getMessage(), r.getParameters());
        }
        return r.getMessage();
    }

    private static void assertLogRecords(List<LogRecord> records) {
        List<String> invalidHostMessages = records.stream()
                .filter(r -> r.getMessage() != null)
                .map(HostValidationAllowedHostsTest::formatMessage)
                .filter(m -> m.contains("rejected by the Host Validation filter"))
                .collect(Collectors.toList());
        assertEquals(4, invalidHostMessages.size());

        assertEquals(2, invalidHostMessages.stream()
                .filter(m -> m.contains("Request host localhost"))
                .count());

        assertEquals(1, invalidHostMessages.stream()
                .filter(m -> m.contains("Request host 127.0.0.1"))
                .count());

        assertEquals(1, invalidHostMessages.stream()
                .filter(m -> m.contains("Request host "
                        + "www.verylonghost-name-myservice-mymicroservice-localhost.com".substring(0, 30) + "..."))
                .count());
    }
}
