package io.quarkus.hibernate.orm.data_management;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag(TestTags.DEVMODE)
public class DataInitScriptHotReloadTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("data.sql"));

    @Test
    public void testDataInitScriptHotReload() {
        String expectedName = "data.sql data init script entity";
        assertBodyIs(expectedName);

        String hotReloadExpectedName = "modified data.sql data init script entity";
        TEST.modifyResourceFile("data.sql", s -> s.replace(expectedName, hotReloadExpectedName));
        assertBodyIs(hotReloadExpectedName);
    }

    private void assertBodyIs(String expectedBody) {
        RestAssured.when().get("/orm-init-script/10").then().body(is(expectedBody));
    }
}
