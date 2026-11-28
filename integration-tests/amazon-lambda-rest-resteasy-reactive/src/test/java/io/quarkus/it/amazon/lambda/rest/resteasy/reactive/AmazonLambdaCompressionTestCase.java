package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.body;
import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.headers;
import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.invoke;
import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.staticFileBody;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@TestProfile(CompressionProfile.class)
public class AmazonLambdaCompressionTestCase {

    @Test
    public void testCompressed() throws IOException {
        JsonPath response = invoke("/compression/compressed", "gzip");
        assertThat(headers(response).getFirst("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionResource.MESSAGE);

        response = invoke("/compression/compressed", null);
        assertThat(headers(response).getFirst("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionResource.MESSAGE);

        assertThat(get("/compression/compressed", "gzip")).isEqualTo(CompressionResource.MESSAGE);
    }

    @Test
    public void testUncompressed() throws IOException {
        JsonPath response = invoke("/compression/uncompressed", "gzip");
        assertThat(headers(response).getFirst("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionResource.MESSAGE);

        assertThat(get("/compression/uncompressed", null)).isEqualTo(CompressionResource.MESSAGE);
    }

    @Test
    public void testStaticFile() throws IOException {
        // with a compressor in the pipeline, files are written in chunks instead of a FileRegion
        String expected = staticFileBody();
        assertThat(expected).as("static file spanning several 8 KiB chunks").hasSizeGreaterThan(2 * 8192);

        JsonPath response = invoke("/compression.txt", "gzip");
        assertThat(headers(response).getFirst("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(expected);

        assertThat(get("/compression.txt", "gzip")).isEqualTo(expected);
    }

    @Test
    public void testLarge() throws IOException {
        JsonPath response = invoke("/compression/large", "gzip");
        assertThat(headers(response).getFirst("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionResource.body());

        assertThat(get("/compression/large", "gzip")).isEqualTo(CompressionResource.body());
    }

    @Test
    public void testChunked() throws IOException {
        JsonPath response = invoke("/compression/chunked", "gzip");
        assertThat(headers(response).getFirst("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionResource.body());

        assertThat(get("/compression/chunked", "gzip")).isEqualTo(CompressionResource.body());
    }

    private String get(String path, String contentEncoding) {
        return given()
                .when()
                .get(path)
                .then()
                .statusCode(200)
                .header("Content-Encoding", contentEncoding == null ? nullValue() : equalTo(contentEncoding))
                .extract().asString();
    }
}
