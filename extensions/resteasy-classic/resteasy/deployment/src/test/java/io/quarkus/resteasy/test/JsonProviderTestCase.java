package io.quarkus.resteasy.test;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class JsonProviderTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(JsonResource.class));

    @Test
    public void testArrayWriter() {

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("[\"simple\", \"array\"]").post("/json/array")
                .then()
                .body("[0]", Matchers.equalTo("simple"))
                .body("[1]", Matchers.equalTo("array"))
                .body("[2]", Matchers.equalTo("test"));
    }

    @Test
    public void testObjectWriter() {

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"simple\": \"obj\"}").post("/json/obj")
                .then()
                .body("simple", Matchers.equalTo("obj"))
                .body("test", Matchers.equalTo("testval"));
    }
}
