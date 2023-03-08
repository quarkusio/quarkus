package io.quarkus.opentelemetry.deployment;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenTelemetryDevServicesDatasourcesTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, InMemorySpanExporterProducer.class)
                    .add(new StringAsset("quarkus.datasource.db-kind=h2\n" +
                            "quarkus.datasource.jdbc.telemetry=true\n"),
                            "application.properties"));

    @Test
    void devDatasource() {

        final String greeting1 = "Hello World";
        createGreeting(greeting1);
        verifyNumOfInsertedTraces(1);

        // Test a change in resources that disables OTEL
        TEST.modifyResourceFile("application.properties",
                s -> s + "quarkus.datasource.jdbc.telemetry.enabled=false\n");
        final String greeting2 = "Hi";
        createGreeting(greeting2);
        verifyNumOfInsertedTraces(0);

        // Test a change in resources that enables OTEL
        TEST.modifyResourceFile("application.properties", s -> s.replace("quarkus.datasource.jdbc.telemetry.enabled=false",
                "quarkus.datasource.jdbc.telemetry.enabled=true"));
        final String greeting3 = "Hey";
        createGreeting(greeting3);
        verifyNumOfInsertedTraces(1);
    }

    private void verifyNumOfInsertedTraces(int insertCount) {
        RestAssured.get("/hello/greetings-insert-count").then().statusCode(200)
                .body(Matchers.is(Integer.toString(insertCount)));
    }

    private void createGreeting(String greeting) {
        RestAssured.when().post("/hello/{greeting}", greeting).then()
                .statusCode(200)
                .body(Matchers.is(greeting));
    }

    @Path("/hello")
    public static class HelloResource {

        @Inject
        InMemorySpanExporter exporter;

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
        @Path("/greetings-insert-count")
        public long greetingsInsertCount() {
            return exporter.getFinishedSpanItems().stream()
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

    @ApplicationScoped
    static class InMemorySpanExporterProducer {

        @jakarta.enterprise.inject.Produces
        @Singleton
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }
}
