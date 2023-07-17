package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared.SharedEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.User;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.subpackage.OtherUserInSubPackage;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * See https://github.com/quarkusio/quarkus/issues/13722
 */
public class MultiplePersistenceUnitsImportSqlHotReloadScriptTest {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(Plane.class.getPackage().getName())
                    .addPackage(SharedEntity.class.getPackage().getName())
                    .addPackage(User.class.getPackage().getName())
                    .addPackage(OtherUserInSubPackage.class.getPackage().getName())
                    .addClass(MultiplePersistenceUnitsSqlLoadScriptTestResource.class)
                    .addAsResource("application-multiple-persistence-units-annotations.properties", "application.properties")
                    .addAsResource("import-sharedentity.sql", "import.sql"));

    @Test
    public void testImportSqlScriptHotReload() {
        String expectedName = "import.sql load script entity";
        assertBodyIs(expectedName);

        String hotReloadExpectedName = "modified import.sql load script entity";
        TEST.modifyResourceFile("import.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(expectedName, hotReloadExpectedName);
            }
        });
        assertBodyIs(hotReloadExpectedName);
    }

    private void assertBodyIs(String expectedBody) {
        RestAssured.when().get("/multiple-persistence-units/orm-sql-load-script/2").then().body(is(expectedBody));
    }

}
