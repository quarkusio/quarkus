package io.quarkus.it.rest.client.multipart;

import static io.quarkus.it.rest.client.multipart.MultipartResource.HELLO_WORLD;
import static io.quarkus.it.rest.client.multipart.MultipartResource.NUMBER;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class MultipartResourceTest {

    private static final String EXPECTED_CONTENT_DISPOSITION_PART = "Content-Disposition: form-data; name=\"%s\"";
    private static final String EXPECTED_CONTENT_TYPE_PART = "Content-Type: %s";

    @Test
    public void shouldSendByteArrayAsBinaryFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/byte-array-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendNullByteArrayAsBinaryFile() {
        // @formatter:off
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
        .when().get("/client/byte-array-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendBufferAsBinaryFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/buffer-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendNullBufferAsBinaryFile() {
        // @formatter:off
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
        .when().get("/client/buffer-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendFileAsBinaryFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/file-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendNullFileAsBinaryFile() {
        // @formatter:off
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
        .when().get("/client/file-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendPathAsBinaryFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/path-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendNullPathAsBinaryFile() {
        // @formatter:off
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
        .when().get("/client/path-as-binary-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendByteArrayAsTextFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/byte-array-as-text-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendBufferAsTextFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/buffer-as-text-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendFileAsTextFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/file-as-text-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendPathAsTextFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/path-as-text-file")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendByteArrayAndPojo() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
        .when().get("/client/byte-array-as-binary-file-with-pojo")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true,pojoOk:true"));
        // @formatter:on
    }

    @Test
    public void shouldSendByteArrayAndPojoWithNullPojo() {
        // @formatter:off
        given()
                .queryParam("withPojo", "false")
                .header("Content-Type", "text/plain")
        .when().get("/client/byte-array-as-binary-file-with-pojo")
        .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true,pojoOk:null"));
        // @formatter:on
    }

    @Test
    public void shouldProducesMultipartForm() {
        String response = RestAssured.get("/produces/multipart")
                .then()
                .contentType(ContentType.MULTIPART)
                .statusCode(200)
                .extract().asString();

        assertMultipartResponseContains(response, "number", MediaType.TEXT_PLAIN, NUMBER);
        assertMultipartResponseContains(response, "file", MediaType.TEXT_PLAIN, HELLO_WORLD);
    }

    private void assertMultipartResponseContains(String response, String name, String contentType, Object value) {
        String[] lines = response.split("--");
        assertThat(lines).anyMatch(line -> line.contains(String.format(EXPECTED_CONTENT_DISPOSITION_PART, name))
                && line.contains(String.format(EXPECTED_CONTENT_TYPE_PART, contentType))
                && line.contains(value.toString()));
    }
}
