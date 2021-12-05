package io.quarkus.hibernate.reactive.panache.test;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class PanacheHotReloadTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testAddNewFieldToEntity() {
        String expectedName = "{\"id\":1,\"name\":\"my name\"}";
        assertBodyIs(expectedName);

        TEST.modifySourceFile(MyEntity.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("public String name;", "public String name;public String tag;");
            }
        });
        TEST.modifyResourceFile("import.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(";", ";\nUPDATE MyEntity SET tag = 'related' WHERE id = 1;\n");
            }
        });
        String hotReloadExpectedName = "{\"id\":1,\"name\":\"my name\",\"tag\":\"related\"}";
        assertBodyIs(hotReloadExpectedName);
    }

    @Test
    public void testAddEntity() {
        RestAssured.when().get("/other-entity/1").then().statusCode(404);

        TEST.addSourceFile(MyOtherEntity.class);
        TEST.addSourceFile(MyOtherTestResource.class);

        TEST.modifyResourceFile("import.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s + s.replaceAll("MyEntity", "MyOtherEntity");
            }
        });
        RestAssured.when().get("/other-entity/1").then().statusCode(200).body(is("{\"id\":1,\"name\":\"my name\"}"));
    }

    private void assertBodyIs(String expectedBody) {
        RestAssured.when().get("/entity/1").then().statusCode(200).body(is(expectedBody));
    }
}
