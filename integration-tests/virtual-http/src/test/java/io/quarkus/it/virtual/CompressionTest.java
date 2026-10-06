package io.quarkus.it.virtual;

import static io.quarkus.it.virtual.FunctionInvoker.body;
import static io.quarkus.it.virtual.FunctionInvoker.get;
import static io.quarkus.it.virtual.FunctionInvoker.staticFileBody;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.microsoft.azure.functions.HttpResponseMessage;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CompressionProfile.class)
public class CompressionTest {

    @Test
    public void testCompressed() throws IOException {
        HttpResponseMessage response = get("/compression/compressed", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);

        response = get("/compression/compressed", null);
        assertThat(response.getHeader("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);
    }

    @Test
    public void testUncompressed() throws IOException {
        HttpResponseMessage response = get("/compression/uncompressed", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);
    }

    @Test
    public void testStaticFile() throws IOException {
        String expected = staticFileBody();
        assertThat(expected).as("static file spanning several 8 KiB chunks").hasSizeGreaterThan(2 * 8192);

        HttpResponseMessage response = get("/compression.txt", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(expected);
    }

    @Test
    public void testLarge() throws IOException {
        HttpResponseMessage response = get("/compression/large", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }

    @Test
    public void testChunked() throws IOException {
        HttpResponseMessage response = get("/compression/chunked", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }
}
