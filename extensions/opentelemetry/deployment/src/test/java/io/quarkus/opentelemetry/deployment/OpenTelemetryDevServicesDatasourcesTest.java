package io.quarkus.opentelemetry.deployment;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenTelemetryDevServicesDatasourcesTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .add(new StringAsset(
                            """
                                    quarkus.datasource.db-kind=h2
                                    quarkus.datasource.jdbc.telemetry=true
                                    quarkus.otel.traces.exporter=test-span-exporter
                                    quarkus.otel.metrics.exporter=none
                                    quarkus.otel.bsp.export.timeout=1s
                                    quarkus.otel.bsp.schedule.delay=50
                                    """),
                            "application.properties"));

    @Test
    void devDatasource() {

        final String greeting1 = "Hello World";
        createGreeting(greeting1);
        verifyNumOfInsertedTraces(6, 1);

        // Test a change in resources that disables OTEL
        TEST.modifyResourceFile("application.properties",
                s -> s + "quarkus.datasource.jdbc.telemetry=false\n");
        final String greeting2 = "Hi";
        createGreeting(greeting2);
        verifyNumOfInsertedTraces(0, 0);
    }

    private void verifyNumOfInsertedTraces(int expectedSpans, int insertCount) {
        ThrowingRunnable assertInsertCount = () -> RestAssured
                .get("/hello/greetings-insert-count/" + expectedSpans)
                .then()
                .statusCode(200)
                .body(Matchers.is(Integer.toString(insertCount)));
        Awaitility.await().atMost(Duration.ofMinutes(1)).untilAsserted(assertInsertCount);
    }

    private void createGreeting(String greeting) {
        RestAssured.when().post("/hello/{greeting}", greeting).then()
                .statusCode(200)
                .body(Matchers.is(greeting));
    }

    @Path("/hello")
    public static class HelloResource {

        @Inject
        TestSpanExporter spanExporter;

        @Inject
        AgroalDataSource dataSource;

        private int idGenerator = 0;

        @Produces(MediaType.TEXT_PLAIN)
        @Transactional
        @POST
        @Path("{greeting}")
        public Response createGreeting(@PathParam("greeting") String greeting) {
            try (Statement stmt = dataSource.getConnection().createStatement()) {
                stmt.executeUpdate(String.format("INSERT INTO greeting VALUES('%d', '%s')", ++idGenerator, greeting));
            } catch (SQLException exception) {
                throw new IllegalStateException(exception);
            }
            return Response.ok(greeting).build();
        }

        @GET
        @Path("/greetings-insert-count/{expectedSpans}")
        public long greetingsInsertCount(@PathParam("expectedSpans") int expectedSpans) {
            List<SpanData> spans = spanExporter.getFinishedSpanItems(expectedSpans);
            return spans.stream()
                    .filter(span -> "INSERT".equals(span.getAttributes().get(AttributeKey.stringKey("db.operation")))).count();
        }

        @PostConstruct
        public void setup() throws Exception {

            try (Connection con = dataSource.getConnection()) {
                try (Statement statement = con.createStatement()) {
                    try {
                        statement.execute("DROP TABLE greeting");
                    } catch (Exception ignored) {

                    }
                    statement.execute("CREATE TABLE greeting (id int, greeting varchar)");
                }
            }
        }
    }
}
