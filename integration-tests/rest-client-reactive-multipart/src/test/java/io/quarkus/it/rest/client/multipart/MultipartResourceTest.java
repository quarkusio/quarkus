package io.quarkus.it.rest.client.multipart;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MultipartResourceTest {

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
}
