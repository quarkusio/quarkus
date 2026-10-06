package io.quarkus.it.gcp.functions.http;

import static io.quarkus.it.gcp.functions.http.FunctionInvoker.body;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;

@QuarkusTest
@TestProfile(CompressionConfigProfile.class)
public class CompressionConfigTest {

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
    public void testCompressors() throws IOException {
        Response response = function.get("/compression/chunked", "gzip, deflate");
        assertThat(response.header("Content-Encoding")).isEqualTo("deflate");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());

        response = function.get("/compression/chunked", "gzip");
        assertThat(response.header("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }

    @Test
    public void testContentSizeThreshold() throws IOException {
        Response response = function.get("/compression/compressed", "deflate");
        assertThat(response.header("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);

        response = function.get("/compression/large", "deflate");
        assertThat(response.header("Content-Encoding")).isEqualTo("deflate");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }
}
