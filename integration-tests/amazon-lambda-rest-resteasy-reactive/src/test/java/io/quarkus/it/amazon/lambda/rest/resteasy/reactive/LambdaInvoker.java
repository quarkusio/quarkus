package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.restassured.path.json.JsonPath;

final class LambdaInvoker {

    private LambdaInvoker() {
    }

    static JsonPath invoke(String path, String acceptEncoding) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath(path);
        request.setMultiValueHeaders(new Headers());
        if (acceptEncoding != null) {
            request.getMultiValueHeaders().add("Accept-Encoding", acceptEncoding);
        }
        JsonPath response = given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("statusCode", equalTo(200))
                .extract().jsonPath();
        assertThat(headers(response).getFirst("Transfer-Encoding")).isNull();
        return response;
    }

    static Headers headers(JsonPath response) {
        Headers headers = new Headers();
        headers.putAll(response.getMap("multiValueHeaders"));
        return headers;
    }

    static String body(JsonPath response) throws IOException {
        String contentEncoding = headers(response).getFirst("Content-Encoding");
        if (contentEncoding == null) {
            assertThat(response.getBoolean("isBase64Encoded")).isFalse();
            return response.getString("body");
        }
        assertThat(response.getBoolean("isBase64Encoded")).isTrue();
        byte[] body = Base64.getDecoder().decode(response.getString("body"));
        try (InputStream in = decode(contentEncoding, body)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String staticFileBody() throws IOException {
        try (InputStream in = LambdaInvoker.class.getResourceAsStream("/META-INF/resources/compression.txt")) {
            assertThat(in).as("compression.txt fixture").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream decode(String contentEncoding, byte[] body) throws IOException {
        if ("gzip".equals(contentEncoding)) {
            return new GZIPInputStream(new ByteArrayInputStream(body));
        }
        assertThat(contentEncoding).isEqualTo("deflate");
        return new InflaterInputStream(new ByteArrayInputStream(body));
    }
}
