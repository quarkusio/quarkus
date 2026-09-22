package io.quarkus.hibernate.orm.data_management;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * With the "none" data management strategy and a schema managed by another tool (schema management strategy "none"),
 * the data init script (data.sql) is not executed on startup,
 * but it stays available to explicit {@link org.hibernate.relational.SchemaManager} calls.
 */
public class DataManagementStrategyNoneWithSchemaManagementNoneTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, PopulateTestResource.class, MyEntity.class,
                            PreexistingSchemaH2Database.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("data-management-none-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none");

    @Test
    public void dataInitScriptNotExecutedOnStartupButAvailableToSchemaManager() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));

        RestAssured.when().post("/orm-populate").then().statusCode(204);

        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));
    }

    @Path("/orm-populate")
    public static class PopulateTestResource {
        @Inject
        EntityManagerFactory entityManagerFactory;

        @POST
        public void populate() {
            entityManagerFactory.unwrap(SessionFactory.class).getSchemaManager().populate();
        }
    }
}
