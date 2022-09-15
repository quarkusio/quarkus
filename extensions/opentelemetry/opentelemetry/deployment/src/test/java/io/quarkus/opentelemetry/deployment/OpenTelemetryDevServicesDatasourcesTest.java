package io.quarkus.opentelemetry.deployment;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;

public class OpenTelemetryDevServicesDatasourcesTest {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DevResource.class)
                    .add(new StringAsset(
                            "quarkus.datasource.db-kind=h2\n" +
                                    "quarkus.datasource.jdbc.driver=io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver"),
                            "application.properties"));

    @Test
    void devDatasource() {
        RestAssured.when().get("/config/{name}", "quarkus.datasource.jdbc.url").then()
                .statusCode(200)
                .body(Matchers.startsWith("jdbc:otel:h2"));

        // Test a change in resources that disables OTEL
        TEST.modifyResourceFile("application.properties", s -> "quarkus.datasource.db-kind=h2\n");
        RestAssured.when().get("/config/{name}", "quarkus.datasource.jdbc.url").then()
                .statusCode(200)
                .body(Matchers.startsWith("jdbc:h2"));

        // Test a change in resources that enables OTEL
        TEST.modifyResourceFile("application.properties", s -> "quarkus.datasource.db-kind=h2\n" +
                "quarkus.datasource.jdbc.driver=io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver");
        RestAssured.when().get("/config/{name}", "quarkus.datasource.jdbc.url").then()
                .statusCode(200)
                .body(Matchers.startsWith("jdbc:otel:h2"));
    }

    @Path("/config")
    public static class DevResource {
        @Inject
        SmallRyeConfig config;
        @Inject
        DataSource dataSource;

        @GET
        @Path("{name}")
        public Response get(@PathParam("name") String name) {
            return Response.ok().entity(config.getRawValue(name)).build();
        }
    }
}
