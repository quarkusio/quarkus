package io.quarkus.flyway.test;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.CoreMatchers;
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
 * When Flyway manages the schema and the data management strategy is "none",
 * the Hibernate ORM data init script (data.sql) is not executed on startup,
 * but it is executed once Flyway has reset the schema when the database is reset from the Dev UI.
 */
public class FlywayDevModeDataInitScriptStrategyNoneTest extends DevUIJsonRPCTest {

    public FlywayDevModeDataInitScriptStrategyNoneTest() {
        super("quarkus-datasource");
    }

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, Fruit.class)
                    .addAsResource("db/data-init-script/V1.0.0__Fruit.sql")
                    .addAsResource("data-init-script-data.sql", "data.sql")
                    .addAsResource(new StringAsset("""
                            quarkus.flyway.migrate-at-start=true
                            quarkus.flyway.locations=db/data-init-script
                            quarkus.hibernate-orm.schema-management.strategy=none
                            quarkus.hibernate-orm.data-management.strategy=none
                            """), "application.properties"));

    @Test
    public void testDataInitScriptExecutedOnResetOnly() throws Exception {
        RestAssured.get("fruit").then().statusCode(200)
                .body("size()", Matchers.is(0));

        Map<String, Object> params = Map.of("ds", "<default>");
        JsonNode success = super.executeJsonRPCMethod("reset", params);
        Assertions.assertTrue(success.asBoolean());

        RestAssured.get("fruit").then().statusCode(200)
                .body("size()", Matchers.is(1))
                .body("[0].name", CoreMatchers.is("Orange"));
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
