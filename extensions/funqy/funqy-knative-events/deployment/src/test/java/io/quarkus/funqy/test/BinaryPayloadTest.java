package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class BinaryPayloadTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BinaryPayload.class));

    private static final byte[] IN = new byte[] { 0x12, 0x34, 0x56, 0x78 };
    private static final String IN_AS_BASE_64 = Base64.getEncoder().encodeToString(IN);
    private static final byte[] OUT = new byte[] { 0x24, 0x68, (byte) 0xAC, (byte) 0xf0 };
    private static final String OUT_AS_BASE_64 = Base64.getEncoder().encodeToString(OUT);

    @Test
    void testBinaryEncoding() {

        byte[] ba = RestAssured.given().contentType("application/octet-stream")
                .header("ce-specversion", "1.0")
                .header("ce-id", "test-id")
                .header("ce-type", "test-type")
                .header("ce-source", "test-source")
                .body(IN)
                .post("/")
                .then()
                .statusCode(200)
                .extract().response().getBody().asByteArray();

        Assertions.assertArrayEquals(OUT, ba);
    }

    @Test
    void testStructuredEncodingV1() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(STRUCTURED_ENCODED_EVENT_V1_1_BODY)
                .post("/")
                .then()
                .statusCode(200)
                .body("data_base64", equalTo(OUT_AS_BASE_64));
    }

    @Test
    void testStructuredEncodingV03() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(STRUCTURED_ENCODED_EVENT_V03_BODY)
                .post("/")
                .then()
                .statusCode(200)
                .body("datacontentencoding", equalTo("base64"))
                .body("data", equalTo(OUT_AS_BASE_64));
    }

    private static final String STRUCTURED_ENCODED_EVENT_V1_1_BODY = "{ \"id\" : \"test-id\", " +
            "  \"specversion\": \"1.1\", " +
            "  \"source\": \"test-source\", " +
            "  \"type\": \"test-type\", " +
            "  \"data_base64\": \"" + IN_AS_BASE_64 + "\" " +
            "}";

    private static final String STRUCTURED_ENCODED_EVENT_V03_BODY = "{ \"id\" : \"test-id\", " +
            "  \"specversion\": \"0.3\", " +
            "  \"source\": \"test-source\", " +
            "  \"type\": \"test-type\", " +
            "  \"datacontentencoding\": \"base64\", " +
            "  \"data\": \"" + IN_AS_BASE_64 + "\" " +
            "}";
}
