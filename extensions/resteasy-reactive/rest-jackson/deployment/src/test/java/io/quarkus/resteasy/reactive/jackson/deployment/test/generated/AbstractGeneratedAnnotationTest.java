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

public abstract class AbstractGeneratedAnnotationTest {

    // --- @JsonProperty + @JsonIgnore ---

    @Test
    public void testPropertyIgnoreSerialization() {
        RestAssured.get("/generated/property-ignore")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("display_name", Matchers.is("Alice"))
                .body("years_old", Matchers.is(25))
                .body(not(containsString("secret")))
                .body(not(containsString("hidden-value")));
    }

    @Test
    public void testPropertyIgnoreRoundTrip() {
        given()
                .contentType("application/json")
                .body("{\"display_name\":\"Bob\",\"years_old\":30}")
                .when()
                .post("/generated/property-ignore")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("display_name", Matchers.is("Bob"))
                .body("years_old", Matchers.is(30))
                .body(not(containsString("secret")));
    }

    @Test
    public void testPropertyIgnoreList() {
        given()
                .contentType("application/json")
                .body("[{\"display_name\":\"A\",\"years_old\":1},{\"display_name\":\"B\",\"years_old\":2}]")
                .when()
                .post("/generated/property-ignore-list")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].display_name", Matchers.is("A"))
                .body("[0].years_old", Matchers.is(1))
                .body("[1].display_name", Matchers.is("B"))
                .body("[1].years_old", Matchers.is(2))
                .body(not(containsString("secret")));
    }

    // --- @JsonNaming + @JsonProperty + @JsonIgnore ---

    @Test
    public void testNamingWithOverrideSerialization() {
        RestAssured.get("/generated/naming-override")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("John"))
                .body("last_name", Matchers.is("Doe"))
                .body("email", Matchers.is("john@example.com"))
                .body(not(containsString("internalId")))
                .body(not(containsString("internal_id")))
                .body(not(containsString("INT-001")));
    }

    @Test
    public void testNamingWithOverrideRoundTrip() {
        given()
                .contentType("application/json")
                .body("{\"first_name\":\"Jane\",\"last_name\":\"Roe\",\"email\":\"jane@test.com\"}")
                .when()
                .post("/generated/naming-override")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("Jane"))
                .body("last_name", Matchers.is("Roe"))
                .body("email", Matchers.is("jane@test.com"))
                .body(not(containsString("internalId")))
                .body(not(containsString("internal_id")));
    }

    // --- @JsonCreator + @JsonAlias + @JsonProperty ---

    @Test
    public void testCreatorAliasWithPrimaryNames() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"Alice\",\"code\":\"A1\"}")
                .when()
                .post("/generated/creator-alias")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("Alice"))
                .body("code", Matchers.is("A1"));
    }

    @Test
    public void testCreatorAliasWithAliasNames() {
        given()
                .contentType("application/json")
                .body("{\"fullName\":\"Bob\",\"identifier\":\"B2\"}")
                .when()
                .post("/generated/creator-alias")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("Bob"))
                .body("code", Matchers.is("B2"));
    }

    @Test
    public void testCreatorAliasWithSecondAlias() {
        given()
                .contentType("application/json")
                .body("{\"display_name\":\"Charlie\",\"code\":\"C3\"}")
                .when()
                .post("/generated/creator-alias")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("Charlie"))
                .body("code", Matchers.is("C3"));
    }

    // --- @JsonView + @JsonIgnore ---

    @Test
    public void testViewIgnoreWithoutView() {
        RestAssured.get("/generated/view-ignore")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicField", Matchers.is("visible"))
                .body("privateField", Matchers.is(42))
                .body(not(containsString("ignoredField")))
                .body(not(containsString("ignored-value")));
    }

    @Test
    public void testViewIgnorePublicView() {
        RestAssured.get("/generated/view-ignore-public")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicField", Matchers.is("visible"))
                .body(not(containsString("privateField")))
                .body(not(containsString("ignoredField")));
    }

    @Test
    public void testViewIgnorePrivateView() {
        RestAssured.get("/generated/view-ignore-private")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicField", Matchers.is("visible"))
                .body("privateField", Matchers.is(42))
                .body(not(containsString("ignoredField")));
    }

    // --- @JsonUnwrapped + @JsonProperty + @JsonIgnore ---

    @Test
    public void testUnwrappedWithRenameSerialization() {
        RestAssured.get("/generated/unwrapped-rename")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("label", Matchers.is("test"))
                .body("city", Matchers.is("NYC"))
                .body("zip_code", Matchers.is("10001"))
                .body(not(containsString("hidden")))
                .body(not(containsString("secret")))
                .body(not(containsString("address")));
    }

    // --- @JsonAnySetter + @JsonIgnoreProperties + @JsonProperty ---

    @Test
    public void testAnySetterIgnoreProperties() {
        given()
                .contentType("application/json")
                .body("{\"id\":\"123\",\"name\":\"test\",\"extra1\":\"val1\",\"removed\":\"gone\",\"deleted\":\"gone\"}")
                .when()
                .post("/generated/any-setter-ignore-props")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("id", Matchers.is("123"))
                .body("name", Matchers.is("test"))
                .body("extras_size", Matchers.is(1));
    }

    @Test
    public void testAnySetterIgnorePropertiesOnlyExtras() {
        given()
                .contentType("application/json")
                .body("{\"id\":\"456\",\"name\":\"test2\",\"extra1\":\"a\",\"extra2\":\"b\",\"extra3\":\"c\"}")
                .when()
                .post("/generated/any-setter-ignore-props")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("id", Matchers.is("456"))
                .body("name", Matchers.is("test2"))
                .body("extras_size", Matchers.is(3));
    }

    // --- @JsonTypeInfo + @JsonSubTypes + @JsonTypeName + @JsonProperty ---

    @Test
    public void testPolymorphicTextSerialization() {
        RestAssured.get("/generated/polymorphic-text")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("kind", Matchers.is("text"))
                .body("text_value", Matchers.is("hello"))
                .body("format", Matchers.is("plain"));
    }

    @Test
    public void testPolymorphicNumberSerialization() {
        RestAssured.get("/generated/polymorphic-number")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("kind", Matchers.is("number"))
                .body("num_value", Matchers.is(42));
    }

    @Test
    public void testPolymorphicListSerialization() {
        RestAssured.get("/generated/polymorphic-list")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].kind", Matchers.is("text"))
                .body("[0].text_value", Matchers.is("first"))
                .body("[0].format", Matchers.is("html"))
                .body("[1].kind", Matchers.is("number"))
                .body("[1].num_value", Matchers.is(99));
    }

    // --- @JsonProperty + @JsonAlias + @JsonIgnoreProperties ---

    @Test
    public void testMultiAnnotationRecordWithPrimaryNames() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"My Title\",\"summary\":\"A summary\",\"is_active\":true}")
                .when()
                .post("/generated/multi-annotation-record")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("title", Matchers.is("My Title"))
                .body("summary", Matchers.is("A summary"))
                .body("is_active", Matchers.is(true));
    }

    @Test
    public void testMultiAnnotationRecordWithAlias() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"Title\",\"desc\":\"A description\",\"is_active\":false}")
                .when()
                .post("/generated/multi-annotation-record")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("title", Matchers.is("Title"))
                .body("summary", Matchers.is("A description"))
                .body("is_active", Matchers.is(false));
    }

    @Test
    public void testMultiAnnotationRecordWithDescriptionAlias() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"T\",\"description\":\"Full desc\",\"is_active\":true}")
                .when()
                .post("/generated/multi-annotation-record")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("title", Matchers.is("T"))
                .body("summary", Matchers.is("Full desc"))
                .body("is_active", Matchers.is(true));
    }

    @Test
    public void testMultiAnnotationRecordIgnoresUnknown() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"T\",\"summary\":\"S\",\"is_active\":true,\"unknown_field\":\"ignored\"}")
                .when()
                .post("/generated/multi-annotation-record")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("title", Matchers.is("T"))
                .body("summary", Matchers.is("S"))
                .body("is_active", Matchers.is(true));
    }

    // --- @JsonCreator + @JsonIgnore + @JsonProperty ---

    @Test
    public void testCreatorIgnoreRoundTrip() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"test\",\"value\":42}")
                .when()
                .post("/generated/creator-ignore")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body("value", Matchers.is(42))
                .body(not(containsString("computed")))
                .body(not(containsString("test:42")));
    }

    // --- @JsonValue + @JsonCreator ---

    @Test
    public void testValueCreatorSerialization() {
        RestAssured.get("/generated/value-creator")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("\"hello world\""));
    }

    // --- @JsonNaming + @JsonAlias ---

    @Test
    public void testNamingAliasSerialization() {
        RestAssured.get("/generated/naming-alias")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("John"))
                .body("last_name", Matchers.is("Doe"));
    }

    @Test
    public void testNamingAliasWithNamingStrategyKeys() {
        given()
                .contentType("application/json")
                .body("{\"first_name\":\"Jane\",\"last_name\":\"Roe\"}")
                .when()
                .post("/generated/naming-alias")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("Jane"))
                .body("last_name", Matchers.is("Roe"));
    }

    @Test
    public void testNamingAliasWithSurnameAlias() {
        given()
                .contentType("application/json")
                .body("{\"first_name\":\"Jane\",\"surname\":\"Roe\"}")
                .when()
                .post("/generated/naming-alias")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("Jane"))
                .body("last_name", Matchers.is("Roe"));
    }

    @Test
    public void testNamingAliasWithFamilyNameAlias() {
        given()
                .contentType("application/json")
                .body("{\"first_name\":\"Jane\",\"familyName\":\"Roe\"}")
                .when()
                .post("/generated/naming-alias")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("Jane"))
                .body("last_name", Matchers.is("Roe"));
    }

    // --- @JsonProperty + @JsonView ---

    @Test
    public void testPropertyViewWithoutView() {
        RestAssured.get("/generated/property-view")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("display_name", Matchers.is("Alice"))
                .body("secret_code", Matchers.is("SECRET"))
                .body("category", Matchers.is("A"));
    }

    @Test
    public void testPropertyViewPublic() {
        RestAssured.get("/generated/property-view-public")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("display_name", Matchers.is("Alice"))
                .body("category", Matchers.is("A"))
                .body(not(containsString("secret_code")))
                .body(not(containsString("SECRET")));
    }

    @Test
    public void testPropertyViewPrivate() {
        RestAssured.get("/generated/property-view-private")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("display_name", Matchers.is("Alice"))
                .body("secret_code", Matchers.is("SECRET"))
                .body("category", Matchers.is("A"));
    }

    // --- @JsonNaming + @JsonView + @JsonProperty ---

    @Test
    public void testNamingViewWithoutView() {
        RestAssured.get("/generated/naming-view")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("Jane"))
                .body("last_name", Matchers.is("Smith"))
                .body("e_mail", Matchers.is("jane@example.com"));
    }

    @Test
    public void testNamingViewPublic() {
        RestAssured.get("/generated/naming-view-public")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("Jane"))
                .body("e_mail", Matchers.is("jane@example.com"))
                .body(not(containsString("last_name")))
                .body(not(containsString("Smith")));
    }

    @Test
    public void testNamingViewPrivate() {
        RestAssured.get("/generated/naming-view-private")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first_name", Matchers.is("Jane"))
                .body("last_name", Matchers.is("Smith"))
                .body("e_mail", Matchers.is("jane@example.com"));
    }

    // --- @JsonIgnoreProperties + @JsonProperty ---

    @Test
    public void testIgnorePropertiesCreatorRecordRoundTrip() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"item\",\"value\":10,\"temp\":\"ignored\",\"debug\":\"ignored\"}")
                .when()
                .post("/generated/ignore-props-creator")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("item"))
                .body("value", Matchers.is(10))
                .body(not(containsString("temp")))
                .body(not(containsString("debug")));
    }

    @Test
    public void testIgnorePropertiesCreatorRecordRejectsUnknown() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"item\",\"value\":10,\"truly_unknown\":\"fail\"}")
                .when()
                .post("/generated/ignore-props-creator")
                .then()
                .statusCode(400);
    }

    // --- @JsonIgnore + @JsonAnySetter + @JsonProperty ---

    @Test
    public void testIgnoreAnySetterWithExtras() {
        given()
                .contentType("application/json")
                .body("{\"name\":\"test\",\"extra1\":\"a\",\"extra2\":\"b\"}")
                .when()
                .post("/generated/ignore-any-setter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", CoreMatchers.is("test"))
                .body("others_size", CoreMatchers.is(2));
    }

    @Test
    public void testIgnoreAnySetterHiddenFieldCapturedByAnySetter() {
        // @JsonIgnore makes the property unknown to regular deserialization,
        // so @JsonAnySetter captures it along with other unknown keys
        given()
                .contentType("application/json")
                .body("{\"name\":\"test\",\"hidden\":\"secret\",\"extra1\":\"a\"}")
                .when()
                .post("/generated/ignore-any-setter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", CoreMatchers.is("test"))
                .body("others_size", CoreMatchers.is(2));
    }

    // --- @JsonAnyGetter ---

    @Test
    public void testAnyGetterSerialization() {
        RestAssured.get("/generated/any-getter")
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
        given()
                .contentType("application/json")
                .body("{\"name\":\"hello\",\"key1\":\"val1\",\"key2\":\"val2\"}")
                .when()
                .post("/generated/any-getter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", CoreMatchers.is("hello"))
                .body("props_size", CoreMatchers.is(2));
    }

    // --- @JsonManagedReference + @JsonBackReference ---

    @Test
    public void testManagedBackReferenceSerialization() {
        RestAssured.get("/generated/managed-reference")
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
        RestAssured.get("/generated/format")
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
                .post("/generated/format")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("round-trip"))
                .body("shape", Matchers.is(2));
    }

    // --- @JsonGetter + @JsonSetter ---

    @Test
    public void testGetterSetterSerialization() {
        RestAssured.get("/generated/getter-setter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("label", Matchers.is("test"))
                .body("count", Matchers.is(5))
                .body(not(containsString("\"name\"")));
    }

    @Test
    public void testGetterSetterDeserialization() {
        given()
                .contentType("application/json")
                .body("{\"label\":\"hello\",\"count\":10}")
                .when()
                .post("/generated/getter-setter")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("label", Matchers.is("hello"))
                .body("count", Matchers.is(10));
    }

    // --- @JsonIgnoreType ---

    @Test
    public void testIgnoreTypeSerialization() {
        RestAssured.get("/generated/ignore-type")
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
        RestAssured.get("/generated/include-all-set")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body("nullableField", Matchers.is("present"))
                .body("emptyField", Matchers.is("not-empty"));
    }

    @Test
    public void testIncludeNullAndEmptyExcluded() {
        RestAssured.get("/generated/include-nulls")
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
        String body = RestAssured.get("/generated/property-order")
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

    // --- Package-protected class with single int constructor ---

    @Test
    public void testPackageProtectedSerialization() {
        RestAssured.get("/generated/package-protected")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("value", Matchers.is(42))
                .body("label", Matchers.is("custom-label"));
    }

    @Test
    public void testPackageProtectedDeserialization() {
        given()
                .contentType("application/json")
                .body("{\"value\":7,\"label\":\"from-json\"}")
                .when()
                .post("/generated/package-protected")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("value", Matchers.is(7))
                .body("label", Matchers.is("from-json"));
    }

    // --- @JsonRawValue ---

    @Test
    public void testRawValueSerialization() {
        String body = RestAssured.get("/generated/raw-value")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body("rawJson.nested", Matchers.is("value"))
                .body("rawJson.count", Matchers.is(1))
                .extract()
                .asString();

        assertTrue(body.contains("\"nested\":\"value\""),
                "Raw JSON should be embedded directly, not escaped");
    }
}
