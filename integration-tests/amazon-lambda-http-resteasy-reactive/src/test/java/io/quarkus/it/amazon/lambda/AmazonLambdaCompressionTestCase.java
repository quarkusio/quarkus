package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@TestProfile(CompressionProfile.class)
public class AmazonLambdaCompressionTestCase {

    @Test
    public void testCompressed() throws IOException {
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();
        request.setRawPath("/compressed");
        request.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        request.getRequestContext().setHttp(new APIGatewayV2HTTPEvent.RequestContext.Http());
        request.getRequestContext().getHttp().setMethod("GET");
        request.setHeaders(Map.of("Accept-Encoding", "gzip"));

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
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(response.getMap("headers"));
        assertEquals("gzip", headers.get("Content-Encoding"));
        assertTrue(response.getBoolean("isBase64Encoded"));
        byte[] gzipped = Base64.getDecoder().decode(response.getString("body"));
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            assertEquals(CompressionResource.MESSAGE, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }

        String body = given()
                .when()
                .get("/compressed")
                .then()
                .statusCode(200)
                .header("Content-Encoding", "gzip")
                .extract().asString();
        assertEquals(CompressionResource.MESSAGE, body);
    }
}
