package io.quarkus.it.virtual;

import static io.quarkus.it.virtual.FunctionInvoker.body;
import static io.quarkus.it.virtual.FunctionInvoker.get;
import static io.quarkus.it.virtual.FunctionInvoker.staticFileBody;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.microsoft.azure.functions.HttpResponseMessage;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CompressionDisabledTest {

    @Test
    public void testCompressionDisabled() throws IOException {
        HttpResponseMessage response = get("/compression/compressed", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.MESSAGE);

        response = get("/compression.txt", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(staticFileBody());

        response = get("/compression/chunked", "gzip");
        assertThat(response.getHeader("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionRoutes.body());
    }
}
