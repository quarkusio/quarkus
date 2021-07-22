package io.quarkus.hibernate.orm;

import static org.hamcrest.Matchers.is;

import java.util.function.Function;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.deployment.devmode.tests.TestStatus;
import io.quarkus.vertx.http.testrunner.ContinuousTestingTestUtils;
import io.restassured.RestAssured;

public class HibernateHotReloadTestCase {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .add(new StringAsset(
                            //TODO: we can't use devservices here because of issues with the class loading
                            //sometimes the external application.properties is picked up and sometimes it isn't
                            ContinuousTestingTestUtils.appProperties(
                                    "quarkus.hibernate-orm.database.generation=drop-and-create",
                                    "quarkus.datasource.jdbc.url=jdbc:h2:mem:test",
                                    "%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:testrunner")),
                            "application.properties")
                    .addAsResource("import.sql"))
            .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HibernateET.class)
                    .addAsResource(new StringAsset("INSERT INTO MyEntity(id, name) VALUES(1, 'TEST ENTITY');"), "import.sql"));

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

    @Test
    public void testImportSqlWithContinuousTesting() {
        ContinuousTestingTestUtils utils = new ContinuousTestingTestUtils();

        TestStatus ts = utils.waitForNextCompletion();

        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        TEST.modifyTestResourceFile("import.sql", new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("TEST ENTITY", "new entity");
            }
        });
        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(1L, ts.getTestsFailed());
        Assertions.assertEquals(0L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

        TEST.modifyTestSourceFile(HibernateET.class, new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("TEST ENTITY", "new entity");
            }
        });
        ts = utils.waitForNextCompletion();
        Assertions.assertEquals(0L, ts.getTestsFailed());
        Assertions.assertEquals(1L, ts.getTestsPassed());
        Assertions.assertEquals(0L, ts.getTestsSkipped());

    }

    private void assertBodyIs(String expectedBody) {
        RestAssured.when().get("/my-entity/2").then().body(is(expectedBody));
    }
}
