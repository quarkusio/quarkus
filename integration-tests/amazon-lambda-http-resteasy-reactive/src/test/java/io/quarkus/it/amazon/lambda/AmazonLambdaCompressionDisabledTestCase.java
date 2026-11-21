package io.quarkus.it.amazon.lambda;

import static io.quarkus.it.amazon.lambda.LambdaInvoker.body;
import static io.quarkus.it.amazon.lambda.LambdaInvoker.headers;
import static io.quarkus.it.amazon.lambda.LambdaInvoker.invoke;
import static io.quarkus.it.amazon.lambda.LambdaInvoker.staticFileBody;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;

@QuarkusTest
public class AmazonLambdaCompressionDisabledTestCase {

    @Test
    public void testCompressionDisabled() throws IOException {
        JsonPath response = invoke("/compression/compressed", "gzip");
        assertThat(headers(response)).doesNotContainKey("Content-Encoding");
        assertThat(body(response)).isEqualTo(CompressionResource.MESSAGE);

        response = invoke("/compression.txt", "gzip");
        assertThat(headers(response)).doesNotContainKey("Content-Encoding");
        assertThat(body(response)).isEqualTo(staticFileBody());

        response = invoke("/compression/chunked", "gzip");
        assertThat(headers(response)).doesNotContainKey("Content-Encoding");
        assertThat(body(response)).isEqualTo(CompressionResource.body());
    }
}
