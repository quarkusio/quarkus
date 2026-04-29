package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class AbstractUnsupportedAnnotationTest {

    // --- @JsonAnyGetter ---

    @Test
    public void testAnyGetterSerialization() {
        // @JsonAnyGetter serializes map entries as flat top-level properties
        RestAssured.get("/unsupported/any-getter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body("color", Matchers.is("red"))
                .body("size", Matchers.is("large"))
                .body(not(containsString("properties")));
    }

    @Test
    public void testAnyGetterDeserialization() {
        // Flat properties are captured by @JsonAnySetter
        given()
                .contentType("application/json")
                .body("{\"name\":\"hello\",\"key1\":\"val1\",\"key2\":\"val2\"}")
                .when()
                .post("/unsupported/any-getter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", CoreMatchers.is("hello"))
                .body("props_size", CoreMatchers.is(2));
    }

    // --- @JsonAutoDetect ---

    @Test
    public void testAutoDetectFieldVisibility() {
        // With fieldVisibility=ANY and getterVisibility=NONE, fields are serialized directly
        RestAssured.get("/unsupported/auto-detect")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("visibleField", Matchers.is("hello"))
                .body("count", Matchers.is(42));
    }

    // --- @JsonManagedReference + @JsonBackReference ---

    @Test
    public void testManagedBackReferenceSerialization() {
        // Parent serializes child, but child does NOT serialize back-reference to parent
        RestAssured.get("/unsupported/managed-reference")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("parentName", Matchers.is("parent"))
                .body("child.childName", Matchers.is("child"))
                .body("child", not(hasKey("parent")));
    }

    // --- @JsonFormat ---

    @Test
    public void testFormatEnumAsNumberSerialization() {
        // @JsonFormat(shape=NUMBER) serializes enum as ordinal
        RestAssured.get("/unsupported/format")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("shape-test"))
                .body("shape", Matchers.is(1));
    }

    @Test
    public void testFormatEnumAsNumberDeserialization() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"round-trip\",\"shape\":2}")
                .when()
                .post("/unsupported/format")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("round-trip"))
                .body("shape", Matchers.is(2));
    }

    // --- @JsonGetter + @JsonSetter ---

    @Test
    public void testGetterSetterSerialization() {
        // @JsonGetter("label") renames the output property
        RestAssured.get("/unsupported/getter-setter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("label", Matchers.is("test"))
                .body("count", Matchers.is(5))
                .body(not(containsString("\"name\"")));
    }

    @Test
    public void testGetterSetterDeserialization() {
        // @JsonSetter("label") accepts the renamed input
        given()
                .contentType("application/json")
                .body("{\"label\":\"hello\",\"count\":10}")
                .when()
                .post("/unsupported/getter-setter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("label", Matchers.is("hello"))
                .body("count", Matchers.is(10));
    }

    // --- @JsonIgnoreType ---

    @Test
    public void testIgnoreTypeSerialization() {
        // Field of @JsonIgnoreType type is excluded from serialization
        RestAssured.get("/unsupported/ignore-type")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("visible"))
                .body(not(containsString("metadata")))
                .body(not(containsString("secret-data")));
    }

    // --- @JsonInclude ---

    @Test
    public void testIncludeAllFieldsPresent() {
        // When all fields are set, all appear in output
        RestAssured.get("/unsupported/include-all-set")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body("nullableField", Matchers.is("present"))
                .body("emptyField", Matchers.is("not-empty"));
    }

    @Test
    public void testIncludeNullAndEmptyExcluded() {
        // @JsonInclude(NON_NULL) excludes null fields,
        // @JsonInclude(NON_EMPTY) excludes empty strings
        RestAssured.get("/unsupported/include-nulls")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body(not(containsString("nullableField")))
                .body(not(containsString("emptyField")));
    }

    // --- @JsonPropertyOrder ---

    @Test
    public void testPropertyOrderSerialization() {
        // @JsonPropertyOrder({"zebra","alpha","middle"}) controls output key ordering
        String body = RestAssured.get("/unsupported/property-order")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("alpha", Matchers.is("a"))
                .body("middle", Matchers.is("m"))
                .body("zebra", Matchers.is("z"))
                .extract()
                .asString();

        int zebraPos = body.indexOf("\"zebra\"");
        int alphaPos = body.indexOf("\"alpha\"");
        int middlePos = body.indexOf("\"middle\"");
        assertTrue(zebraPos < alphaPos, "zebra should appear before alpha");
        assertTrue(alphaPos < middlePos, "alpha should appear before middle");
    }

    // --- @JsonRawValue ---

    @Test
    public void testRawValueSerialization() {
        // @JsonRawValue outputs the string as raw JSON (not escaped)
        String body = RestAssured.get("/unsupported/raw-value")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body("rawJson.nested", Matchers.is("value"))
                .body("rawJson.count", Matchers.is(1))
                .extract()
                .asString();

        // Verify the raw JSON is embedded directly, not escaped as a string
        assertTrue(body.contains("\"nested\":\"value\""),
                "Raw JSON should be embedded directly, not escaped");
    }
}
