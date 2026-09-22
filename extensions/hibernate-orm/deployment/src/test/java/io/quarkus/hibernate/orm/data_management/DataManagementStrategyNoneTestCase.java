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
 * With the "none" data management strategy while Hibernate ORM creates the schema (the default in tests),
 * the data init script (data.sql) is not executed on startup, although the schema init script (import.sql) is,
 * but it stays available to explicit {@link org.hibernate.relational.SchemaManager} calls.
 */
public class DataManagementStrategyNoneTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, SchemaManagerTestResource.class, MyEntity.class))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none");

    @Test
    public void dataInitScriptNotExecutedOnStartupButAvailableToSchemaManager() {
        // The schema init script was executed as part of the schema creation, the data init script was not
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is("default sql load script entity"));
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));

        // populate() executes the data init script
        RestAssured.when().post("/orm-schema-manager/populate").then().statusCode(204);
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));

        // truncate() clears the tables, then executes the data init script again
        RestAssured.when().post("/orm-schema-manager/truncate").then().statusCode(204);
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));
    }

    @Path("/orm-schema-manager")
    public static class SchemaManagerTestResource {

        @Inject
        EntityManagerFactory entityManagerFactory;

        @POST
        @Path("/populate")
        public void populate() {
            entityManagerFactory.unwrap(SessionFactory.class).getSchemaManager().populate();
        }

        @POST
        @Path("/truncate")
        public void truncate() {
            entityManagerFactory.unwrap(SessionFactory.class).getSchemaManager().truncate();
        }
    }
}
