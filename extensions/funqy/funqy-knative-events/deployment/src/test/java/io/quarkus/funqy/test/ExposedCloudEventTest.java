package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class ExposedCloudEventTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExposedCloudEvents.class));

    @Test
    public void testVanillaHttp() {
        // when a function handles CloudEvent explicitly, vanilla HTTP is considered to be a bad request.
        RestAssured.given().contentType("application/json")
                .body("{}")
                .post("/doubleIt")
                .then()
                .statusCode(400);
    }

    @Test
    public void testCloudEventAttributeDefaultsForStructuredEncoding() {
        String event = "{ \"id\" : \"test-id\", " +
                "  \"specversion\": \"1.0\", " +
                "  \"source\": \"test-source\", " +
                "  \"type\": \"test-defaults\" " +
                "}";
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event)
                .post("/")
                .then()
                .statusCode(200)
                .body("specversion", equalTo("1.0"))
                .body("id", notNullValue())
                .body("type", equalTo("default-type"))
                .body("source", equalTo("default-source"));
    }

    @Test
    public void testCloudEventAttributeDefaultsForBinaryEncoding() {
        RestAssured.given()
                .header("ce-id", "test-id")
                .header("ce-specversion", "1.0")
                .header("ce-type", "test-defaults")
                .header("ce-source", "test-source")
                .post()
                .then()
                .statusCode(204)
                .header("ce-specversion", equalTo("1.0"))
                .header("ce-id", notNullValue())
                .header("ce-type", equalTo("default-type"))
                .header("ce-source", equalTo("default-source"));
    }

    @Test
    public void testGenericInput() {
        RestAssured.given().contentType("application/json")
                .header("ce-id", "test-id")
                .header("ce-specversion", "1.0")
                .header("ce-type", "test-generics")
                .header("ce-source", "test-source")
                .body("[{\"i\" : 1}, {\"i\" : 2}, {\"i\" : 3}]")
                .then()
                .statusCode(200)
                .body(equalTo("6"));
    }

    @ParameterizedTest
    @MethodSource("provideBinaryEncodingTestArgs")
    public void testBinaryEncoding(Map<String, String> headers, String specversion, String dataSchemaHdrName) {

        RequestSpecification req = RestAssured.given().contentType("application/json");

        for (Map.Entry<String, String> h : headers.entrySet()) {
            req = req.header(h.getKey(), h.getValue());
        }

        req.body(BINARY_ENCODED_EVENT_BODY)
                .post("/")
                .then()
                .statusCode(200)
                .header("ce-specversion", equalTo(specversion))
                .header("ce-id", equalTo("double-it-id"))
                .header("ce-type", equalTo("double-it-type"))
                .header("ce-source", equalTo("/OfDoubleIt"))
                .header(dataSchemaHdrName, equalTo("dataschema-server"))
                .header("ce-extserver", equalTo("ext-server-val"))
                .body("i", equalTo(42))
                .body("s", equalTo("abcabc"));
    }

    @ParameterizedTest
    @MethodSource("provideStructuredEncodingTestArgs")
    public void testStructuredEncoding(String event, String specversion, String dataSchemaFieldName) {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event)
                .post("/")
                .then()
                .statusCode(200)
                .body("specversion", equalTo(specversion))
                .body("id", equalTo("double-it-id"))
                .body("type", equalTo("double-it-type"))
                .body("source", equalTo("/OfDoubleIt"))
                .body(dataSchemaFieldName, equalTo("dataschema-server"))
                .body("extserver", equalTo("ext-server-val"))
                .body("data.i", equalTo(42))
                .body("data.s", equalTo("abcabc"));
    }

    static {
        Map<String, String> common = new HashMap<>();
        common.put("ce-id", "test-id");
        common.put("ce-type", "test-type");
        common.put("ce-source", "/OfTest");
        common.put("ce-subject", "test-subj");
        common.put("ce-time", "2018-04-05T17:31:00Z");
        common.put("ce-extclient", "ext-client-val");

        Map<String, String> v1 = new HashMap<>(common);
        v1.put("ce-specversion", "1.0");
        v1.put("ce-dataschema", "test-dataschema-client");
        BINARY_ENCODED_EVENT_V1_HEADERS = Collections.unmodifiableMap(v1);

        Map<String, String> v1_1 = new HashMap<>(common);
        v1_1.put("ce-specversion", "1.1");
        v1_1.put("ce-dataschema", "test-dataschema-client");
        BINARY_ENCODED_EVENT_V1_1_HEADERS = Collections.unmodifiableMap(v1_1);

        Map<String, String> v03 = new HashMap<>(common);
        v03.put("ce-specversion", "0.3");
        v03.put("ce-schemaurl", "test-dataschema-client");
        BINARY_ENCODED_EVENT_V03_HEADERS = Collections.unmodifiableMap(v03);
    }

    public static final Map<String, String> BINARY_ENCODED_EVENT_V1_HEADERS;
    public static final Map<String, String> BINARY_ENCODED_EVENT_V1_1_HEADERS;
    public static final Map<String, String> BINARY_ENCODED_EVENT_V03_HEADERS;

    private static Stream<Arguments> provideBinaryEncodingTestArgs() {
        return Stream.<Arguments> builder()
                .add(Arguments.arguments(BINARY_ENCODED_EVENT_V1_HEADERS, "1.0", "ce-dataschema"))
                .add(Arguments.arguments(BINARY_ENCODED_EVENT_V1_1_HEADERS, "1.1", "ce-dataschema"))
                .add(Arguments.arguments(BINARY_ENCODED_EVENT_V03_HEADERS, "0.3", "ce-schemaurl"))
                .build();
    }

    public static final String BINARY_ENCODED_EVENT_BODY = " { \"i\" : 21, \"s\" : \"abc\" } ";

    static final String STRUCTURED_ENCODED_EVENT_V1_BODY = "{ \"id\" : \"test-id\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"/OfTest\", " +
            "  \"subject\": \"test-subj\", " +
            "  \"time\": \"2018-04-05T17:31:00Z\", " +
            "  \"type\": \"test-type\", " +
            "  \"extclient\": \"ext-client-val\", " +
            "  \"dataschema\": \"test-dataschema-client\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": { \"i\" : 21, \"s\" : \"abc\" } " +
            "}";

    static final String STRUCTURED_ENCODED_EVENT_V1_1_BODY = "{ \"id\" : \"test-id\", " +
            "  \"specversion\": \"1.1\", " +
            "  \"source\": \"/OfTest\", " +
            "  \"subject\": \"test-subj\", " +
            "  \"time\": \"2018-04-05T17:31:00Z\", " +
            "  \"type\": \"test-type\", " +
            "  \"extclient\": \"ext-client-val\", " +
            "  \"dataschema\": \"test-dataschema-client\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": { \"i\" : 21, \"s\" : \"abc\" } " +
            "}";

    static final String STRUCTURED_ENCODED_EVENT_V03_BODY = "{ \"id\" : \"test-id\", " +
            "  \"specversion\": \"0.3\", " +
            "  \"source\": \"/OfTest\", " +
            "  \"subject\": \"test-subj\", " +
            "  \"time\": \"2018-04-05T17:31:00Z\", " +
            "  \"type\": \"test-type\", " +
            "  \"extclient\": \"ext-client-val\", " +
            "  \"schemaurl\": \"test-dataschema-client\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": { \"i\" : 21, \"s\" : \"abc\" } " +
            "}";

    private static Stream<Arguments> provideStructuredEncodingTestArgs() {
        return Stream.<Arguments> builder()
                .add(Arguments.arguments(STRUCTURED_ENCODED_EVENT_V1_BODY, "1.0", "dataschema"))
                .add(Arguments.arguments(STRUCTURED_ENCODED_EVENT_V1_1_BODY, "1.1", "dataschema"))
                .add(Arguments.arguments(STRUCTURED_ENCODED_EVENT_V03_BODY, "0.3", "schemaurl"))
                .build();
    }
}
