package io.quarkus.flyway.test;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

/**
 * Scripts set through the deprecated "quarkus.hibernate-orm.sql-load-script" property keep their historical behavior
 * when Flyway manages the schema: they are only executed when Hibernate ORM creates the schema,
 * so neither on startup nor once Flyway has reset the schema when the database is reset from the Dev UI.
 */
public class FlywayDevModeDeprecatedSqlLoadScriptTest extends DevUIJsonRPCTest {

    public FlywayDevModeDeprecatedSqlLoadScriptTest() {
        super("quarkus-datasource");
    }

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, Fruit.class)
                    .addAsResource("db/data-init-script/V1.0.0__Fruit.sql")
                    .addAsResource("data-init-script-data.sql", "legacy-load.sql")
                    .addAsResource(new StringAsset("""
                            quarkus.flyway.migrate-at-start=true
                            quarkus.flyway.locations=db/data-init-script
                            quarkus.hibernate-orm.schema-management.strategy=none
                            quarkus.hibernate-orm.sql-load-script=legacy-load.sql
                            """), "application.properties"));

    @Test
    public void testScriptNotExecutedOnStartupNorOnReset() throws Exception {
        RestAssured.get("fruit").then().statusCode(200)
                .body("size()", Matchers.is(0));

        Map<String, Object> params = Map.of("ds", "<default>");
        JsonNode success = super.executeJsonRPCMethod("reset", params);
        Assertions.assertTrue(success.asBoolean());

        RestAssured.get("fruit").then().statusCode(200)
                .body("size()", Matchers.is(0));
    }

    @Path("/fruit")
    public static class Endpoint {

        @Inject
        EntityManager entityManager;

        @GET
        public List<Fruit> list() {
            return entityManager.createQuery("from Fruit", Fruit.class).getResultList();
        }
    }
}
