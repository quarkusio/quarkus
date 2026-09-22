package io.quarkus.hibernate.orm.schema_management;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag(TestTags.DEVMODE)
public class SchemaInitScriptHotReloadTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testSchemaInitScriptHotReload() {
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
        RestAssured.when().get("/orm-init-script/2").then().body(is(expectedBody));
    }
}
