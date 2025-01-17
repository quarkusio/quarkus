package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenTelemetrySuppressNonAppUriManagementInterfaceTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .add(new StringAsset(
                            """
                                    quarkus.otel.traces.exporter=test-span-exporter
                                    quarkus.otel.metrics.exporter=none
                                    quarkus.otel.bsp.export.timeout=1s
                                    quarkus.otel.bsp.schedule.delay=50
                                    quarkus.management.enabled=true
                                    quarkus.management.port=9001
                                    """),
                            "application.properties"));

    @Test
    void test() {

        // Must not be traced
        RestAssured.given()
                .get("http://localhost:9001/q/health/")
                .then()
                .statusCode(200);
        RestAssured.given()
                .get("/q/dev-ui/")
                .then()
                .statusCode(200);
        RestAssured.given()
                .get("/q/dev-ui/icon/font-awesome.js")
                .then()
                .statusCode(200);
        // Valid trace
        RestAssured.given()
                .get("/hello")
                .then()
                .statusCode(200);
        // Get span names
        List<String> spans = Arrays.asList(
                RestAssured.given()
                        .get("/hello/spans")
                        .then()
                        .statusCode(200)
                        .extract().body()
                        .asString()
                        .split(";"));

        assertThat(spans).containsExactly("GET /hello");
    }

    @Path("/hello")
    public static class HelloResource {

        @Inject
        TestSpanExporter spanExporter;

        @GET
        public String greetings() {
            return "Hello test";
        }

        /**
         * Gets a string with the received spans names concatenated by ;
         *
         * @return
         */
        @GET
        @Path("/spans")
        public String greetingsInsertAtLeast() {
            String spanNames = spanExporter.getFinishedSpanItemsAtLeast(1).stream()
                    .map(SpanData::getName)
                    .reduce((s1, s2) -> s1 + ";" + s2).orElse("");
            System.out.println(spanNames);
            return spanNames;
        }
    }
}
