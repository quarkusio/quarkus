package io.quarkus.it.virtual;

import static io.quarkus.it.virtual.FunctionInvoker.body;
import static io.quarkus.it.virtual.FunctionInvoker.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.microsoft.azure.functions.HttpResponseMessage;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CompressionConfigProfile.class)
public class CompressionConfigTest {

    @Test
    public void testCompressors() throws IOException {
        HttpResponseMessage response = get("/compression/chunked", "gzip, deflate");
        assertThat(response.getHeader("Content-Encoding")).isEqualTo("deflate");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());

        response = get("/compression/chunked", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }

    @Test
    public void testContentSizeThreshold() throws IOException {
        HttpResponseMessage response = get("/compression/compressed", "deflate");
        assertThat(response.getHeader("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);

        response = get("/compression/large", "deflate");
        assertThat(response.getHeader("Content-Encoding")).isEqualTo("deflate");
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }
}
