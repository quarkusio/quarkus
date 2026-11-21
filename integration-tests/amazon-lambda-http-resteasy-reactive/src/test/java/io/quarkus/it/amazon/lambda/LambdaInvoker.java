package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.restassured.path.json.JsonPath;

final class LambdaInvoker {

    private LambdaInvoker() {
    }

    static JsonPath invoke(String path, String acceptEncoding) {
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();
        request.setRawPath(path);
        request.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        request.getRequestContext().setHttp(new APIGatewayV2HTTPEvent.RequestContext.Http());
        request.getRequestContext().getHttp().setMethod("GET");
        request.setHeaders(acceptEncoding == null ? Map.of() : Map.of("Accept-Encoding", acceptEncoding));
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
        assertThat(headers(response)).doesNotContainKey("Transfer-Encoding");
        return response;
    }

    static Map<String, String> headers(JsonPath response) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(response.getMap("headers"));
        return headers;
    }

    static String body(JsonPath response) throws IOException {
        String contentEncoding = headers(response).get("Content-Encoding");
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
