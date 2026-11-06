package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@TestProfile(CompressionProfile.class)
public class AmazonLambdaCompressionTestCase {

    @Test
    public void testCompressed() throws IOException {
        JsonPath response = invoke("/compression/compressed");
        assertEquals("gzip", headers(response).getFirst("Content-Encoding"));
        assertTrue(response.getBoolean("isBase64Encoded"));
        byte[] gzipped = Base64.getDecoder().decode(response.getString("body"));
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            assertEquals(CompressionResource.MESSAGE, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }

        String body = given()
                .when()
                .get("/compression/compressed")
                .then()
                .statusCode(200)
                .header("Content-Encoding", "gzip")
                .extract().asString();
        assertEquals(CompressionResource.MESSAGE, body);
    }

    @Test
    public void testUncompressed() {
        JsonPath response = invoke("/compression/uncompressed");
        assertNull(headers(response).getFirst("Content-Encoding"));
        assertFalse(response.getBoolean("isBase64Encoded"));
        assertEquals(CompressionResource.MESSAGE, response.getString("body"));

        given()
                .when()
                .get("/compression/uncompressed")
                .then()
                .statusCode(200)
                .header("Content-Encoding", nullValue())
                .body(equalTo(CompressionResource.MESSAGE));
    }

    @Test
    public void testStaticFile() {
        // with a compressor in the pipeline, files are written in chunks instead of a FileRegion
        String body = given()
                .when()
                .get("/compression.txt")
                .then()
                .statusCode(200)
                .header("Content-Encoding", "gzip")
                .extract().asString();
        assertEquals(CompressionResource.MESSAGE, body);
    }

    private JsonPath invoke(String path) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath(path);
        request.setMultiValueHeaders(new Headers());
        request.getMultiValueHeaders().add("Accept-Encoding", "gzip");
        return given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("statusCode", equalTo(200))
                .extract().jsonPath();
    }

    private Headers headers(JsonPath response) {
        Headers headers = new Headers();
        headers.putAll(response.getMap("multiValueHeaders"));
        return headers;
    }
}
