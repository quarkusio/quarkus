package io.quarkus.it.rest.client.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class MultipartEchoResourceTest {

    @Test
    public void shouldReturnMultipartPojoFromResourceNotRegisteredForReflection() {
        String response = given()
                .multiPart("text", "hello")
                .when().post("/multipart-echo")
                .then()
                .statusCode(200)
                .contentType(ContentType.MULTIPART)
                .extract().asString();

        assertThat(response)
                .contains("Content-Disposition: form-data; name=\"text\"")
                .contains("hello");
    }
}
