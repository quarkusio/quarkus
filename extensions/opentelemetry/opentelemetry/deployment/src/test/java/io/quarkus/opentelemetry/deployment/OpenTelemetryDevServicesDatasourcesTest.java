package io.quarkus.opentelemetry.deployment;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

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

        TEST.modifyResourceFile("application.properties", s -> "quarkus.datasource.db-kind=h2\n");

        RestAssured.when().get("/config/{name}", "quarkus.datasource.jdbc.url").then()
                .statusCode(200)
                .body(Matchers.startsWith("jdbc:h2"));
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
