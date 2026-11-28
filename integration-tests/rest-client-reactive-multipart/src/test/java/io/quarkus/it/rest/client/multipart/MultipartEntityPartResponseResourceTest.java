package io.quarkus.it.rest.client.multipart;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class MultipartEntityPartResponseResourceTest {

    @Test
    public void shouldReturnEntityPartsWrappedInResponse() {
        String response = given()
                .accept(ContentType.MULTIPART)
                .when().get("/multipart-entity-part-response")
                .then()
                .statusCode(200)
                .contentType(ContentType.MULTIPART)
                .extract().asString();

        assertThat(response)
                .contains("Content-Disposition: form-data; name=\"message\"")
                .contains("from response");
    }
}
