package io.quarkus.hibernate.orm.sql_load_script;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

/**
 * As in Quarkus 3, an "import.sql" file is ignored in production mode,
 * so it doesn't enable the deprecated "sql-load-script" behavior there:
 * the SQL init scripts apply, and so does "data-management.strategy".
 */
public class ImportSqlIgnoredInProdModeTestCase {
    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("schema.sql")
                    .addAsResource("data.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-deployment", Version.getVersion())))
            // QuarkusProdModeTest => we lose dev services
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:import-sql-ignored-prod;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
            .overrideConfigKey("quarkus.hibernate-orm.data-management.strategy", "create")
            .setRun(true);

    @Test
    public void importSqlNotExecuted() {
        // Both files insert an entity with this ID: only the one from schema.sql is there
        RestAssured.when().get("/orm-sql-load-script/1").then()
                .body(Matchers.is("default schema init script entity"));
    }

    @Test
    public void dataInitScriptExecuted() {
        RestAssured.when().get("/orm-sql-load-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));
    }
}
