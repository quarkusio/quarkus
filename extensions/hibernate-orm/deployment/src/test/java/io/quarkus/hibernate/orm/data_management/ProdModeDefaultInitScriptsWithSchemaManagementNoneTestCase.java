package io.quarkus.hibernate.orm.data_management;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

/**
 * In production mode, when the schema is managed by another tool (schema management strategy "none"),
 * neither the schema init script nor the data init script is executed by default:
 * the data management strategy defaults to "none" when Hibernate ORM does not create the schema.
 */
public class ProdModeDefaultInitScriptsWithSchemaManagementNoneTestCase {

    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class, PreexistingSchemaH2Database.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-resteasy-deployment", Version.getVersion())))
            // QuarkusProdModeTest => we lose dev services
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("prod-default-init-scripts-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
            .setRun(true);

    @Test
    public void schemaInitScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }

    @Test
    public void dataInitScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }
}
