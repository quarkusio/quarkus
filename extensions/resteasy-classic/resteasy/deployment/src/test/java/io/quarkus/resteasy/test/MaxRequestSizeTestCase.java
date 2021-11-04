package io.quarkus.resteasy.test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class MaxRequestSizeTestCase {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MaxBodySizeResource.class)
                    .addAsResource(new StringAsset("quarkus.http.limits.max-body-size=10"), "application.properties"));

    @Test
    public void testSmallFixedLengthRequest() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("q");
        }
        // while sending a payload within the limit should return 200
        RestAssured.given()
                .body(sb.toString())
                .post("/max-body-size")
                .then().statusCode(200).body(Matchers.equalTo("cl" + sb.toString()));
    }

    @Test
    public void testLargeFixedLengthRequest() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            sb.append("q");
        }

        RestAssured.given()
                .body(sb.toString())
                .post("/max-body-size")
                .then().statusCode(413);

    }

    @Test
    public void testSmallChunkedRequest() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("q");
        }
        // while sending a payload within the limit should return 200
        RestAssured.given()
                .contentType("application/octet-stream")
                .body(new FilterInputStream(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8))) {
                    @Override
                    public int available() throws IOException {
                        return -1;
                    }
                })
                .post("/max-body-size")
                .then().statusCode(200).body(Matchers.equalTo("chunked" + sb.toString()));
    }

    @Test
    public void testChunkedLargeRequest() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            sb.append("q");
        }

        RestAssured.given()
                .contentType("application/octet-stream")
                .body(new FilterInputStream(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8))) {
                    @Override
                    public int available() throws IOException {
                        return -1;
                    }
                })
                .post("/max-body-size")
                .then().statusCode(413);

    }
}
