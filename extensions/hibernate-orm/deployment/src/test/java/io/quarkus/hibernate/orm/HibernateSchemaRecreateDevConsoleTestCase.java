package io.quarkus.hibernate.orm;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag(TestTags.DEVMODE)
public class HibernateSchemaRecreateDevConsoleTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    public HibernateSchemaRecreateDevConsoleTestCase() {
        super("io.quarkus.quarkus-datasource");
    }

    @Test
    public void testCleanDatabase() throws Exception {
        RestAssured.when().get("/my-entity/count").then().body(is("2"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("3"));
        Map<String, Object> params = Map.of("ds", "<default>");
        JsonNode success = super.executeJsonRPCMethod("reset", params);
        assertTrue(success.asBoolean());
        RestAssured.when().get("/my-entity/count").then().body(is("2"));

    }

    private void assertBodyIs(String expectedBody) {
        RestAssured.when().get("/my-entity/2").then().body(is(expectedBody));
    }
}
