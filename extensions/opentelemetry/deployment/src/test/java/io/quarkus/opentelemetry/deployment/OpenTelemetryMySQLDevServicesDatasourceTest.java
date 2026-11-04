package io.quarkus.opentelemetry.deployment;

import java.sql.Connection;
import java.sql.Statement;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenTelemetryMySQLDevServicesDatasourceTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MySQLResource.class)
                    .add(new StringAsset(
                            """
                                    quarkus.datasource.db-kind=mysql
                                    quarkus.datasource.jdbc.telemetry=true
                                    quarkus.otel.metrics.exporter=none
                                    """),
                            "application.properties"));

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
