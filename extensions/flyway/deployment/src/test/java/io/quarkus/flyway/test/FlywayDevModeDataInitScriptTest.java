package io.quarkus.flyway.test;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

/**
 * When Flyway manages the schema, the Hibernate ORM data init script (data.sql) is executed on startup
 * once the migrations have been applied, and executed again when the database is reset from the Dev UI.
 */
public class FlywayDevModeDataInitScriptTest extends DevUIJsonRPCTest {

    public FlywayDevModeDataInitScriptTest() {
        super("quarkus-datasource");
    }

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, Fruit.class)
                    .addAsResource("db/data-init-script/V1.0.0__Fruit.sql")
                    .addAsResource("data-init-script-data.sql", "data.sql")
                    .addAsResource("data-init-script-config.properties", "application.properties"));

    @Test
    public void testDataInitScriptExecutedOnStartupAndOnReset() throws Exception {
        RestAssured.get("fruit").then().statusCode(200)
                .body("size()", Matchers.is(1))
                .body("[0].name", CoreMatchers.is("Orange"));

        RestAssured.delete("fruit").then().statusCode(204);
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

        @DELETE
        @Transactional
        public void deleteAll() {
            entityManager.createQuery("delete from Fruit").executeUpdate();
        }
    }
}
