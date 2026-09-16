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
 * The default init scripts (import.sql and data.sql) are picked up in production mode too,
 * and both are executed when Hibernate ORM creates the schema,
 * without any data management strategy being configured.
 */
public class ProdModeDefaultInitScriptsTestCase {

    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-resteasy-deployment", Version.getVersion())))
            // QuarkusProdModeTest => we lose dev services
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:prod-default-init-scripts;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
            .setRun(true);

    @Test
    public void schemaInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is("default sql load script entity"));
    }

    @Test
    public void dataInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));
    }
}
