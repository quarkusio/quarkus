package io.quarkus.it.smallrye.graphql;

import static io.quarkus.it.smallrye.graphql.PayloadCreator.MEDIATYPE_JSON;
import static io.quarkus.it.smallrye.graphql.PayloadCreator.getPayload;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testEndpoint() {

        String helloRequest = getPayload("{\n" +
                "  hello {\n" +
                "    time\n" +
                "    message\n" +
                "  }\n" +
                "}");

        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(helloRequest)
                .post("/graphql")
                .then()
                .statusCode(200)
                .and()
                .body(containsString("hello"))
                .body(containsString("11:34"));
    }

}
