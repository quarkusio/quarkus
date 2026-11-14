package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

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
        assertThat(headers(response)).containsEntry("Content-Encoding", "gzip");
        assertThat(response.getBoolean("isBase64Encoded")).isTrue();
        byte[] gzipped = Base64.getDecoder().decode(response.getString("body"));
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(CompressionResource.MESSAGE);
        }

        String body = given()
                .when()
                .get("/compression/compressed")
                .then()
                .statusCode(200)
                .header("Content-Encoding", "gzip")
                .extract().asString();
        assertThat(body).isEqualTo(CompressionResource.MESSAGE);
    }

    @Test
    public void testUncompressed() {
        JsonPath response = invoke("/compression/uncompressed");
        assertThat(headers(response)).doesNotContainKey("Content-Encoding");
        assertThat(response.getBoolean("isBase64Encoded")).isFalse();
        assertThat(response.getString("body")).isEqualTo(CompressionResource.MESSAGE);

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
        JsonPath response = invoke("/compression.txt");
        assertThat(headers(response)).containsEntry("Content-Encoding", "gzip").doesNotContainKey("Transfer-Encoding");
        assertThat(response.getBoolean("isBase64Encoded")).isTrue();

        String body = given()
                .when()
                .get("/compression.txt")
                .then()
                .statusCode(200)
                .header("Content-Encoding", "gzip")
                .extract().asString();
        assertThat(body).isEqualTo(CompressionResource.MESSAGE);
    }

    private JsonPath invoke(String path) {
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();
        request.setRawPath(path);
        request.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        request.getRequestContext().setHttp(new APIGatewayV2HTTPEvent.RequestContext.Http());
        request.getRequestContext().getHttp().setMethod("GET");
        request.setHeaders(Map.of("Accept-Encoding", "gzip"));
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

    private Map<String, String> headers(JsonPath response) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(response.getMap("headers"));
        return headers;
    }
}
