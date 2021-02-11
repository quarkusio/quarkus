package io.quarkus.hibernate.orm;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class HibernateHotReloadTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testAddNewFieldToEntity() {
        String expectedName = "MyEntity:import.sql load script entity";
        assertBodyIs(expectedName);

        String hotReloadExpectedName = "MyEntity:import.sql load script entity:new tag";
        TEST.modifySourceFile(MyEntity.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("private String name;", "private String name;public String tag;    " +
                        "public String getTag() {\n" +
                        "        return tag;\n" +
                        "    }\n" +
                        "\n" +
                        "    public void setTag(String tag) {\n" +
                        "        this.tag = tag;\n" +
                        "    }")
                        .replace("\"MyEntity:\" + name", "\"MyEntity:\" + name + \":\" + tag");
            }
        });
        TEST.modifyResourceFile("import.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replaceAll("name\\)", "name,tag)").replaceAll("'\\);", "','new tag');");
            }
        });
        assertBodyIs(hotReloadExpectedName);
    }

    @Test
    public void testAddEntity() {
        RestAssured.when().get("/my-entity/2").then().body(is("MyEntity:import.sql load script entity"));

        TEST.addSourceFile(OtherEntity.class);
        TEST.addSourceFile(OtherEntityTestResource.class);

        TEST.modifyResourceFile("import.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s + s.replaceAll("MyEntity", "OtherEntity");
            }
        });
        RestAssured.when().get("/other-entity/2").then().body(is("OtherEntity:import.sql load script entity"));
    }

    private void assertBodyIs(String expectedBody) {
        RestAssured.when().get("/my-entity/2").then().body(is(expectedBody));
    }
}
