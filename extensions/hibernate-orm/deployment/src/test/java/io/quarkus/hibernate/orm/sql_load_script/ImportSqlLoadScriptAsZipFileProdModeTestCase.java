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
 * In a packaged application, the zip file is inside a jar and cannot be accessed as a path.
 */
public class ImportSqlLoadScriptAsZipFileProdModeTestCase {
    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-load-script-as-zip-file-test.properties", "application.properties")
                    .addAsResource("load-script-test.zip"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-deployment", Version.getVersion())))
            // QuarkusProdModeTest => we lose dev services
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:sql-load-script-zip-prod;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
            .setRun(true);

    @Test
    public void testSqlLoadScriptAsZipFile() {
        String name = "other-load-script sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/3").then().body(Matchers.is(name));
    }
}
