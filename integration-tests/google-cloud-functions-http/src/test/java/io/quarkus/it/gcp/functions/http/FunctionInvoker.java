package io.quarkus.it.gcp.functions.http;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.eclipse.microprofile.config.ConfigProvider;

import com.google.cloud.functions.invoker.runner.Invoker;

import io.quarkus.gcp.functions.http.QuarkusHttpFunction;
import io.restassured.config.DecoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Serves {@link QuarkusHttpFunction} with the Functions Framework invoker, the server that runs deployed Java functions.
 */
class FunctionInvoker implements AutoCloseable {

    private static final RestAssuredConfig RAW = RestAssuredConfig.config()
            .decoderConfig(DecoderConfig.decoderConfig().noContentDecoders());

    private final Invoker invoker;
    private final int port;

    FunctionInvoker() throws Exception {
        // The compression test profiles start only the virtual HTTP server in Quarkus.
        port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", int.class).orElse(8081);
        invoker = new Invoker(port, QuarkusHttpFunction.class.getName(), "http",
                Thread.currentThread().getContextClassLoader());
        invoker.startTestServer();
    }

    Response get(String path, String acceptEncoding) {
        RequestSpecification request = given().config(RAW).port(port);
        if (acceptEncoding != null) {
            request.header("Accept-Encoding", acceptEncoding);
        }
        Response response = request.get(path).then().statusCode(200).extract().response();
        assertThat(response.headers().hasHeaderWithName("Content-Length")
                && response.headers().hasHeaderWithName("Transfer-Encoding"))
                .as("both Content-Length and Transfer-Encoding are set").isFalse();
        return response;
    }

    static String body(Response response) throws IOException {
        try (InputStream in = decode(response.header("Content-Encoding"), response.asByteArray())) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String staticFileBody() throws IOException {
        try (InputStream in = FunctionInvoker.class.getResourceAsStream("/META-INF/resources/compression.txt")) {
            assertThat(in).as("compression.txt fixture").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream decode(String contentEncoding, byte[] body) throws IOException {
        if ("gzip".equals(contentEncoding)) {
            return new GZIPInputStream(new ByteArrayInputStream(body));
        }
        if ("deflate".equals(contentEncoding)) {
            return new InflaterInputStream(new ByteArrayInputStream(body));
        }
        assertThat(contentEncoding).isNull();
        return new ByteArrayInputStream(body);
    }

    @Override
    public void close() throws Exception {
        invoker.stopServer();
    }
}
