package io.quarkus.opentelemetry.deployment;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@DisabledOnOs(OS.WINDOWS)
public class OpenTelemetryMySQLDevServicesDatasourceTest {

    // MySQL is only added to this test's application: with both H2 and MySQL present,
    // the other tests of this module could not infer the datasource db-kind
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MySQLResource.class)
                    .add(new StringAsset(
                            """
                                    quarkus.datasource.db-kind=mysql
                                    quarkus.datasource.jdbc.telemetry=true
                                    quarkus.otel.metrics.exporter=none
                                    """),
                            "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-mysql-deployment", Version.getVersion())));

    @Test
    void mysqlDevServiceStartsWithOpenTelemetry() {
        RestAssured.when().post("/hello/hello").then().statusCode(200);
    }

    @Path("/hello")
    public static class MySQLResource {

        @Inject
        AgroalDataSource dataSource;

        @POST
        @Path("hello")
        public Response hello() throws Exception {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
            }
            return Response.ok().build();
        }
    }
}
