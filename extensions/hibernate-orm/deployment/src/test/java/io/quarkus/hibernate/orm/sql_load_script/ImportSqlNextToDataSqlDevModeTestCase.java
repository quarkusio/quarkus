package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;

/**
 * Adding or removing "import.sql" or "data.sql" in dev mode changes which behavior applies,
 * so these files are watched even when they are not in use.
 */
@Tag(TestTags.DEVMODE)
public class ImportSqlNextToDataSqlDevModeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class));

    @Test
    public void testAddingImportSqlNextToDataSql() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));

        // 'import.sql' is not in use, but adding it makes the configuration ambiguous
        runner.addResourceFile("import.sql", "INSERT INTO MyEntity(id, name) VALUES(1, 'import.sql entity');");
        RestAssured.when().get("/orm-init-script/10").then()
                .log().ifValidationFails(LogDetail.BODY)
                .statusCode(500)
                .body(Matchers.containsString("which is ambiguous"));

        // Removing 'data.sql' switches to the deprecated behavior enabled by 'import.sql'
        runner.deleteResourceFile("data.sql");
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is("import.sql entity"));
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }
}
