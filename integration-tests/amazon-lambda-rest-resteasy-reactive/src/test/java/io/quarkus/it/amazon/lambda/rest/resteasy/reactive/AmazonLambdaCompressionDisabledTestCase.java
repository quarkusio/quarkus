package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.body;
import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.headers;
import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.invoke;
import static io.quarkus.it.amazon.lambda.rest.resteasy.reactive.LambdaInvoker.staticFileBody;
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
        assertThat(headers(response).getFirst("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionResource.MESSAGE);

        response = invoke("/compression.txt", "gzip");
        assertThat(headers(response).getFirst("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(staticFileBody());

        response = invoke("/compression/chunked", "gzip");
        assertThat(headers(response).getFirst("Content-Encoding")).isNull();
        assertThat(body(response)).isEqualTo(CompressionResource.body());
    }
}
