package io.quarkus.hibernate.orm;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class HibernateSchemaRecreateDevConsoleTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testCleanDatabase() {
        RestAssured.when().get("/my-entity/count").then().body(is("2"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("3"));
        RestAssured.with()
                .redirects().follow(false).formParam("name", "<default>").post("q/dev/io.quarkus.quarkus-datasource/reset")
                .then()
                .statusCode(303);
        RestAssured.when().get("/my-entity/count").then().body(is("2"));

    }

    private void assertBodyIs(String expectedBody) {
        RestAssured.when().get("/my-entity/2").then().body(is(expectedBody));
    }
}
