package io.quarkus.it.amazon.lambda.rest.funqy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaV1SimpleTestCase {

    @Test
    public void testSwaggerUi() throws Exception {
        // this tests the FileRegion support in the handler
        given()
                .when()
                .get("/q/swagger-ui/")
                .then()
                .statusCode(200)
                .body(containsString("OpenAPI UI"));
    }

    @Test
    public void test404() throws Exception {
        given()
                .when()
                .get("/nowhere")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/funqy", "/funqyAsync" })
    public void testFunqy(String path) {
        given()
                .contentType("application/json")
                .accept("application/json")
                .body("\"Bill\"")
                .when()
                .post(path)
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("application/json"))
                .body(equalTo("\"Make it funqy Bill\""));
    }

}
