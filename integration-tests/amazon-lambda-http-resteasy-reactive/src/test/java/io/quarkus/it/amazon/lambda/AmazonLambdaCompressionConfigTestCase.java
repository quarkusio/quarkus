package io.quarkus.it.amazon.lambda;

import static io.quarkus.it.amazon.lambda.LambdaInvoker.body;
import static io.quarkus.it.amazon.lambda.LambdaInvoker.headers;
import static io.quarkus.it.amazon.lambda.LambdaInvoker.invoke;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@TestProfile(CompressionConfigProfile.class)
public class AmazonLambdaCompressionConfigTestCase {

    @Test
    public void testCompressors() throws IOException {
        JsonPath response = invoke("/compression/chunked", "gzip, deflate");
        assertThat(headers(response)).containsEntry("Content-Encoding", "deflate");
        assertThat(body(response)).isEqualTo(CompressionResource.body());

        response = invoke("/compression/chunked", "gzip");
        assertThat(headers(response)).doesNotContainKey("Content-Encoding");
        assertThat(body(response)).isEqualTo(CompressionResource.body());
    }

    @Test
    public void testContentSizeThreshold() throws IOException {
        JsonPath response = invoke("/compression/compressed", "deflate");
        assertThat(headers(response)).doesNotContainKey("Content-Encoding");
        assertThat(body(response)).isEqualTo(CompressionResource.MESSAGE);

        response = invoke("/compression/large", "deflate");
        assertThat(headers(response)).containsEntry("Content-Encoding", "deflate");
        assertThat(body(response)).isEqualTo(CompressionResource.body());
    }
}
