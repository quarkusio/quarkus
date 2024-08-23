package io.quarkus.it.rest.client.http2.multipart;

import static io.quarkus.it.rest.client.http2.multipart.MultipartResource.HELLO_WORLD;
import static io.quarkus.it.rest.client.http2.multipart.MultipartResource.NUMBER;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import jakarta.ws.rs.core.MediaType;

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
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/byte-array-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/byte-array-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
    }

    @Test
    public void shouldSendNullByteArrayAsBinaryFile() {
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/byte-array-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/params/byte-array-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
    }

    @Test
    public void shouldSendBufferAsBinaryFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/buffer-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/buffer-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
    }

    @Test
    public void shouldSendNullBufferAsBinaryFile() {
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/buffer-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/params/buffer-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
    }

    @Test
    public void shouldSendFileAsBinaryFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/file-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/file-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
    }

    @Test
    public void shouldMultiAsBinaryFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/multi-byte-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/multi-byte-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
    }

    @Test
    public void shouldSendNullFileAsBinaryFile() {
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/file-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/params/file-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
    }

    @Test
    public void shouldSendPathAsBinaryFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/path-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/path-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,nameOk:true"));
    }

    @Test
    public void shouldSendNullPathAsBinaryFile() {
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/path-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
        given()
                .queryParam("nullFile", "true")
                .header("Content-Type", "text/plain")
                .when().get("/client/params/path-as-binary-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:null,nameOk:true"));
    }

    @Test
    public void shouldSendByteArrayAsTextFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/byte-array-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/byte-array-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
    }

    @Test
    public void shouldSendBufferAsTextFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/buffer-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/buffer-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
    }

    @Test
    public void shouldSendFileAsTextFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/file-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/file-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
    }

    @Test
    public void shouldSendPathAsTextFile() {
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/path-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/params/path-as-text-file")
                .then()
                .statusCode(200)
                .body(equalTo("fileOk:true,numberOk:true"));
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

    @Test
    public void shouldProperlyHandleOctetStreamFile() {
        // @formatter:off
        given()
                .header("Content-Type", "text/plain")
                .when().get("/client/octet-stream")
                .then()
                .statusCode(200)
                .body(equalTo("test"));
        // @formatter:on
    }

    @Test
    public void shouldProducesInputStreamRestResponse() {
        RestAssured.get("/produces/input-stream-rest-response")
                .then()
                .contentType(ContentType.TEXT)
                .statusCode(200)
                .body(equalTo("HELLO WORLD"));
    }

    private void assertMultipartResponseContains(String response, String name, String contentType, Object value) {
        String[] lines = response.split("--");
        assertThat(lines).anyMatch(line -> line.contains(String.format(EXPECTED_CONTENT_DISPOSITION_PART, name))
                && line.contains(String.format(EXPECTED_CONTENT_TYPE_PART, contentType))
                && line.contains(value.toString()));
    }
}
