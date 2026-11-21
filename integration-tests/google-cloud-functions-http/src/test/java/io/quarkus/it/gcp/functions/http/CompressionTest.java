package io.quarkus.it.gcp.functions.http;

import static io.quarkus.it.gcp.functions.http.FunctionInvoker.body;
import static io.quarkus.it.gcp.functions.http.FunctionInvoker.staticFileBody;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

@QuarkusTest
@TestProfile(CompressionProfile.class)
public class CompressionTest {

    private static FunctionInvoker function;

    @BeforeAll
    static void startFunction() throws Exception {
        function = new FunctionInvoker();
    }

    @AfterAll
    static void stopFunction() throws Exception {
        function.close();
    }

    @Test
    public void testCompressed() throws IOException {
        Response response = function.get("/compression/compressed", "gzip");
        assertThat(response.header("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);

        response = function.get("/compression/compressed", null);
        assertThat(response.header("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);
    }

    @Test
    public void testUncompressed() throws IOException {
        Response response = function.get("/compression/uncompressed", "gzip");
        assertThat(response.header("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);
    }

    @Test
    public void testStaticFile() throws IOException {
        String expected = staticFileBody();
        assertThat(expected).as("static file spanning several 8 KiB chunks").hasSizeGreaterThan(2 * 8192);

        Response response = function.get("/compression.txt", "gzip");
        assertThat(response.header("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(expected);
    }

    @Test
    public void testLarge() throws IOException {
        Response response = function.get("/compression/large", "gzip");
        assertThat(response.header("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }

    @Test
    public void testChunked() throws IOException {
        Response response = function.get("/compression/chunked", "gzip");
        assertThat(response.header("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }
}
