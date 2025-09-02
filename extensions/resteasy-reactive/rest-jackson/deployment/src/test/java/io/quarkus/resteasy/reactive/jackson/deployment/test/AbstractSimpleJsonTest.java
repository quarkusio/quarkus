package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.security.test.utils.TestIdentityController;
import io.restassured.RestAssured;

public abstract class AbstractSimpleJsonTest {

    @Test
    public void testJson() {
        doTestGetPersonNoSecurity("/simple", "/person");

        RestAssured
                .with()
                .body("{\"first\": \"Bob\", \"last\": \"Builder\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("content-length", notNullValue())
                .header("transfer-encoding", nullValue())
                .body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured
                .with()
                .body("{\"first\": \"Bob\", \"last\": \"Builder\"}")
                .contentType("application/vnd.quarkus.person-v1+json")
                .post("/simple/person-custom-mt")
                .then()
                .statusCode(200)
                .contentType("application/vnd.quarkus.person-v1+json")
                .body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured
                .with()
                .body("{\"first\": \"Bob\", \"last\": \"Builder\"}")
                .contentType("application/vnd.quarkus.person-v1+json")
                .post("/simple/person-custom-mt-response")
                .then()
                .statusCode(201)
                .contentType("application/vnd.quarkus.person-v1+json")
                .body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured
                .with()
                .body("{\"first\": \"Bob\", \"last\": \"Builder\"}")
                .contentType("application/vnd.quarkus.person-v1+json")
                .post("/simple/person-custom-mt-response-with-type")
                .then()
                .statusCode(201)
                .contentType("application/vnd.quarkus.other-v1+json")
                .body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured
                .with()
                .body("[{\"first\": \"Bob\", \"last\": \"Builder\"}, {\"first\": \"Bob2\", \"last\": \"Builder2\"}]")
                .contentType("application/json; charset=utf-8")
                .post("/simple/people")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[1].first", Matchers.equalTo("Bob"))
                .body("[1].last", Matchers.equalTo("Builder"))
                .body("[0].first", Matchers.equalTo("Bob2"))
                .body("[0].last", Matchers.equalTo("Builder2"));

        RestAssured.with()
                .body("[\"first\", \"second\"]")
                .contentType("application/json; charset=utf-8")
                .post("/simple/strings")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[0]", Matchers.equalTo("first"))
                .body("[1]", Matchers.equalTo("second"));

        RestAssured
                .with()
                .body("[{\"first\": \"Bob\", \"last\": \"Builder\"}, {\"first\": \"Bob2\", \"last\": \"Builder2\"}]")
                .contentType("application/json; charset=utf-8")
                .post("/simple/super")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[1].first", Matchers.equalTo("Bob"))
                .body("[1].last", Matchers.equalTo("Builder"))
                .body("[0].first", Matchers.equalTo("Bob2"))
                .body("[0].last", Matchers.equalTo("Builder2"));
    }

    private void doTestGetPersonNoSecurity(final String basePath, String path) {
        RestAssured.get(basePath + path)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body("first", Matchers.equalTo("Bob"))
                .body("last", Matchers.equalTo("Builder"));
    }

    @Test
    public void testLargeJsonPost() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; ++i) {
            sb.append("abc");
        }
        String longString = sb.toString();
        RestAssured
                .with()
                .body("{\"first\": \"" + longString + "\", \"last\": \"" + longString + "\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/person-large")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first", Matchers.equalTo(longString)).body("last", Matchers.equalTo(longString));
    }

    @Test
    public void testValidatedJson() {
        String postBody = "{\"first\": \"Bob\", \"last\": \"Builder\"}";
        RestAssured
                .with()
                .body(postBody)
                .accept("application/json")
                .contentType("application/json")
                .post("/simple/person-validated")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured
                .with()
                .body(postBody)
                .accept("application/json")
                .contentType("application/json")
                .post("/simple/person-invalid-result")
                .then()
                .statusCode(500)
                .contentType("application/json");

        RestAssured
                .with()
                .body("{\"first\": \"Bob\"}")
                .accept("application/json")
                .contentType("application/json")
                .post("/simple/person-validated")
                .then()
                .statusCode(400)
                .contentType("application/json");
    }

    @Test
    public void testAsyncJson() {
        RestAssured.get("/simple/async-person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));
    }

    // when there is no view defined, Jackson will serialize all fields
    @Test
    public void testUserWithoutView() {
        RestAssured.get("/simple/user-without-view")
                .then().body(containsString("1"), containsString("test"));
    }

    @Test
    public void testUserWithPublicView() {
        RestAssured.get("/simple/user-with-public-view")
                .then().body(not(containsString("1")), containsString("test"));
    }

    @Test
    public void testUserWithPrivateView() {
        RestAssured.get("/simple/user-with-private-view")
                .then().body(containsString("1"), containsString("test"));
    }

    @Test
    public void testPerClassExceptionMapper() {
        RestAssured
                .with()
                .body("{\"first\": Bob, \"last\": \"Builder\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/person")
                .then()
                .statusCode(400)
                .contentType("application/json")
                .body(containsString("Unrecognized token 'Bob'"));
    }

    @Test
    public void testJsonMulti() {
        RestAssured
                .with()
                .get("/simple/multi2")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].first", Matchers.equalTo("Bob"))
                .body("[0].last", Matchers.equalTo("Builder"))
                .body("[1].first", Matchers.equalTo("Bob2"))
                .body("[1].last", Matchers.equalTo("Builder2"));
        RestAssured
                .with()
                .get("/simple/multi1")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].first", Matchers.equalTo("Bob"))
                .body("[0].last", Matchers.equalTo("Builder"));
        RestAssured
                .with()
                .get("/simple/multi0")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(Matchers.equalTo("[]"));
    }

    @Test
    public void testCustomSerialization() {
        assertEquals(0, SimpleJsonResource.UnquotedFieldsPersonSerialization.count.intValue());

        // assert that we get a proper response
        // we can't use json-path to assert because the returned string is not proper json as it does not have quotes around the field names
        RestAssured.get("/simple/custom-serialized-person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(containsString("Builder"));
        // assert that our bi-function was created
        assertEquals(1, SimpleJsonResource.UnquotedFieldsPersonSerialization.count.intValue());

        // assert with a list of people
        RestAssured
                .with()
                .body("[{\"first\": \"Bob\", \"last\": \"Builder\"}, {\"first\": \"Bob2\", \"last\": \"Builder2\"}]")
                .contentType("application/json; charset=utf-8")
                .post("/simple/custom-serialized-people")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(containsString("Builder"))
                .body(containsString("Bob2"))
                .body(containsString("Builder2"));
        // assert that another instance of our bi-function was created as a different resource method was used
        assertEquals(2, SimpleJsonResource.UnquotedFieldsPersonSerialization.count.intValue());

        RestAssured.get("/simple/custom-serialized-person")
                .then()
                .statusCode(200)
                .contentType("application/json");
        RestAssured
                .with()
                .body("[{\"first\": \"Bob\", \"last\": \"Builder\"}, {\"first\": \"Bob2\", \"last\": \"Builder2\"}]")
                .contentType("application/json; charset=utf-8")
                .post("/simple/custom-serialized-people")
                .then()
                .statusCode(200)
                .contentType("application/json");
        // assert that the instances were re-used as we simply invoked methods that should have already created their object writers
        assertEquals(2, SimpleJsonResource.UnquotedFieldsPersonSerialization.count.intValue());

        RestAssured.get("/simple/invalid-use-of-custom-serializer")
                .then()
                .statusCode(500);
        // a new instance should have been created
        assertEquals(3, SimpleJsonResource.UnquotedFieldsPersonSerialization.count.intValue());
    }

    @Test
    public void testCustomDeserialization() {
        int currentCounter = SimpleJsonResource.UnquotedFieldsPersonDeserialization.count.intValue();

        // assert that the reader support the unquoted fields (because we have used a custom object reader
        // via `@CustomDeserialization`
        Person actual = RestAssured.given()
                .body("{first: \"Hello\", last: \"Deserialization\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/custom-deserialized-person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .extract().as(Person.class);
        assertEquals("Hello", actual.getFirst());
        assertEquals("Deserialization", actual.getLast());
        assertEquals(currentCounter + 1, SimpleJsonResource.UnquotedFieldsPersonDeserialization.count.intValue());

        // assert that the instances were re-used as we simply invoked methods that should have already created their object readers
        RestAssured.given()
                .body("{first: \"Hello\", last: \"Deserialization\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/custom-deserialized-person")
                .then()
                .statusCode(200);
        assertEquals(currentCounter + 1, SimpleJsonResource.UnquotedFieldsPersonDeserialization.count.intValue());

        // assert with a list of people
        RestAssured
                .with()
                .body("[{first: \"Bob\", last: \"Builder\"}, {first: \"Bob2\", last: \"Builder2\"}]")
                .contentType("application/json; charset=utf-8")
                .post("/simple/custom-serialized-people")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(containsString("Builder"))
                .body(containsString("Bob2"))
                .body(containsString("Builder2"));
    }

    @Test
    public void testSecurityDisabledPerson() {
        doTestGetPersonNoSecurity("/other", "/no-security");
    }

    @Test
    public void testSecurePerson() {
        doTestSecurePerson("/simple", "/secure-person");
    }

    @Test
    public void testSecurePersonWithPrivateView() {
        doTestSecurePerson("/simple", "/secure-person-with-private-view");
    }

    @Test
    public void testSecurePersonWithPublicView() {
        doTestSecurePersonWithPublicView("/simple", "/secure-person-with-public-view");
    }

    @Test
    public void testUniSecurePersonWithPublicView() {
        doTestSecurePersonWithPublicView("/simple", "/uni-secure-person-with-public-view");
    }

    @Test
    public void testSecurePersonFromAbstract() {
        doTestSecurePerson("/other", "/abstract-with-security");
    }

    @Test
    public void testSecureUniPerson() {
        doTestSecurePerson("/simple", "/secure-uni-person");
    }

    @Test
    public void testSecureRestResponsePerson() {
        doTestSecurePerson("/simple", "/secure-rest-response-person");
    }

    @Test
    public void testSecureFieldRolesAllowedConfigExp() {
        TestIdentityController.resetRoles().add("max", "max", "admin");
        RestAssured.given()
                .auth().preemptive().basic("max", "max")
                .get("/simple/secure-person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(containsString("0"))
                .body(containsString("10 Downing St"))
                .body(not(containsString("November 30, 1874")))
                .body(containsString("Builder"));
        TestIdentityController.resetRoles().add("max", "max", "user");
        RestAssured.given()
                .auth().preemptive().basic("max", "max")
                .get("/simple/secure-person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(containsString("0"))
                .body(containsString("10 Downing St"))
                .body(not(containsString("November 30, 1874")))
                .body(not(containsString("Builder")));
        TestIdentityController.resetRoles().add("max", "max", "alice");
        RestAssured.given()
                .auth().preemptive().basic("max", "max")
                .get("/simple/secure-person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(containsString("0"))
                .body(not(containsString("10 Downing St")))
                .body(containsString("November 30, 1874"))
                .body(not(containsString("Builder")));
    }

    private void doTestSecurePerson(String basePath, final String path) {
        RestAssured.get(basePath + path)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(containsString("0"))
                .body(not(containsString("10 Downing St")))
                .body(not(containsString("November 30, 1874")))
                .body(not(containsString("Builder")));
    }

    private void doTestSecurePersonWithPublicView(String basePath, final String path) {
        RestAssured.get(basePath + path)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(not(containsString("0")))
                .body(not(containsString("10 Downing St")))
                .body(not(containsString("November 30, 1874")))
                .body(not(containsString("Builder")));
    }

    @Test
    public void testSecurePeople() {
        doTestSecurePeople("secure-people");
    }

    @Test
    public void testSecureUniPeople() {
        doTestSecurePeople("secure-uni-people");
    }

    private void doTestSecurePeople(final String path) {
        RestAssured.get("/simple/" + path)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
                .body(not(containsString("Builder")));
    }

    @Test
    public void testGenericInput() {
        RestAssured
                .with()
                .body("{\"content\": {\"name\":\"foo\", \"email\":\"bar\"}}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/genericInput")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("foo"));
    }

    @Test
    public void testInterface() {
        RestAssured
                .with()
                .get("/simple/interface")
                .then()
                .statusCode(200)
                .body("nestedInterface.int", Matchers.is(42))
                .body("nestedInterface.character", Matchers.is("a"))
                .body("nestedInterface.string", Matchers.is("response"));
    }

    @Test
    public void testSecureFieldOnAbstractClass() {
        // implementor with / without @SecureField returned
        testSecuredFieldOnAbstractClass("cat", "dog");
        // abstract class with @SecureField returned
        testSecuredFieldOnAbstractClass("abstract-named-cat", "abstract-named-dog");
        // abstract class without @SecureField directly, but with secured field's field returned
        testSecuredFieldOnAbstractClass("abstract-cat", "abstract-dog");
        // interface with implementors that have @SecureField
        testSecuredFieldOnAbstractClass("interface-cat", "interface-dog");
    }

    @Test
    public void testSecureFieldOnlyOnFieldOfReturnTypeField() {
        // returns class with @SecureField is only on field's field
        testSecuredFieldOnReturnTypeField("unsecured-pet");
        // returns abstract class and only @SecureField is on implementor's field's field
        testSecuredFieldOnReturnTypeField("abstract-unsecured-pet");
    }

    @Test
    public void testSecureFieldOnCollectionTypeField() {
        TestIdentityController.resetRoles().add("max", "max", "user");
        RestAssured
                .with()
                .auth().preemptive().basic("max", "max")
                .get("/simple/frog")
                .then()
                .statusCode(200)
                .body("partner", Matchers.notNullValue())
                .body("ponds[0].name", Matchers.is("Atlantic Ocean"))
                .body("ponds[0].waterQuality", Matchers.nullValue());
        TestIdentityController.resetRoles().add("rolfe", "rolfe", "admin");
        RestAssured
                .with()
                .auth().preemptive().basic("rolfe", "rolfe")
                .get("/simple/frog")
                .then()
                .statusCode(200)
                .body("partner", Matchers.notNullValue())
                .body("ponds[0].name", Matchers.is("Atlantic Ocean"))
                .body("ponds[0].waterQuality", Matchers.is("CLEAR"));
    }

    @Test
    public void testSecureFieldOnArrayTypeField() {
        TestIdentityController.resetRoles().add("max", "max", "user");
        RestAssured
                .with()
                .auth().preemptive().basic("max", "max")
                .get("/simple/frog-body-parts")
                .then()
                .statusCode(200)
                .body("parts[0].name", Matchers.nullValue());
        TestIdentityController.resetRoles().add("rolfe", "rolfe", "admin");
        RestAssured
                .with()
                .auth().preemptive().basic("rolfe", "rolfe")
                .get("/simple/frog-body-parts")
                .then()
                .statusCode(200)
                .body("parts[0].name", Matchers.is("protruding eyes"));
    }

    @Test
    public void testSecureFieldOnTypeVariable() {
        TestIdentityController.resetRoles().add("max", "max", "user");
        RestAssured
                .with()
                .auth().preemptive().basic("max", "max")
                .get("/simple/secure-field-on-type-variable")
                .then()
                .statusCode(200)
                .body("entity.prices[0].price", Matchers.nullValue());
        TestIdentityController.resetRoles().add("rolfe", "rolfe", "admin");
        RestAssured
                .with()
                .auth().preemptive().basic("rolfe", "rolfe")
                .get("/simple/secure-field-on-type-variable")
                .then()
                .statusCode(200)
                .body("entity.prices[0].price", Matchers.notNullValue());
    }

    private static void testSecuredFieldOnReturnTypeField(String subPath) {
        TestIdentityController.resetRoles().add("max", "max", "user");
        RestAssured
                .with()
                .auth().preemptive().basic("max", "max")
                .get("/simple/" + subPath)
                .then()
                .statusCode(200)
                .body("publicName", Matchers.is("Unknown"))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.nullValue());
        TestIdentityController.resetRoles().add("rolfe", "rolfe", "admin");
        RestAssured
                .with()
                .auth().preemptive().basic("rolfe", "rolfe")
                .get("/simple/" + subPath)
                .then()
                .statusCode(200)
                .body("publicName", Matchers.is("Unknown"))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.is("VMD"));
    }

    private static void testSecuredFieldOnAbstractClass(String catPath, String dogPath) {
        TestIdentityController.resetRoles().add("max", "max", "user");
        RestAssured
                .with()
                .auth().preemptive().basic("max", "max")
                .get("/simple/" + catPath)
                .then()
                .statusCode(200)
                .body("publicName", Matchers.is("Garfield"))
                .body("initial", Matchers.is("G"))
                .body("privateName", Matchers.nullValue())
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.nullValue())
                .body("privateAge", Matchers.nullValue());
        RestAssured
                .with()
                .auth().preemptive().basic("max", "max")
                .get("/simple/" + dogPath)
                .then()
                .statusCode(200)
                .body("publicName", Matchers.is("Leo"))
                .body("privateName", Matchers.nullValue())
                .body("age", Matchers.is(5))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.nullValue());
        TestIdentityController.resetRoles().add("rolfe", "rolfe", "admin");
        RestAssured
                .with()
                .auth().preemptive().basic("rolfe", "rolfe")
                .get("/simple/" + catPath)
                .then()
                .statusCode(200)
                .body("publicName", Matchers.is("Garfield"))
                .body("initial", Matchers.is("G"))
                .body("privateName", Matchers.is("Monday"))
                .body("privateAge", Matchers.is(4))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.is("VMD"));
        RestAssured
                .with()
                .auth().preemptive().basic("rolfe", "rolfe")
                .get("/simple/" + dogPath)
                .then()
                .statusCode(200)
                .body("publicName", Matchers.is("Leo"))
                .body("privateName", Matchers.is("Jack"))
                .body("age", Matchers.is(5))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.is("VMD"));
    }

    @Test
    public void testEcho() {
        RestAssured
                .with()
                .body("{\"publicName\":\"Leo\",\"veterinarian\":{\"name\":\"Dolittle\"},\"age\":5,\"vaccinated\":true}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/dog-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicName", Matchers.is("Leo"))
                .body("privateName", Matchers.nullValue())
                .body("age", Matchers.is(5))
                .body("vaccinated", Matchers.is(true))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.nullValue());
    }

    @Test
    public void testEchoWithNullString() {
        RestAssured
                .with()
                .body("{\"publicName\":null,\"veterinarian\":{\"name\":\"Dolittle\"},\"age\":5,\"vaccinated\":true}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/dog-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicName", Matchers.nullValue())
                .body("privateName", Matchers.nullValue())
                .body("age", Matchers.is(5))
                .body("vaccinated", Matchers.is(true))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.nullValue());
    }

    @Test
    public void testEchoWithMissingPrimitive() {
        RestAssured
                .with()
                .body("{\"publicName\":\"Leo\",\"veterinarian\":{\"name\":\"Dolittle\"},\"age\":5}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/dog-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("publicName", Matchers.is("Leo"))
                .body("privateName", Matchers.nullValue())
                .body("age", Matchers.is(5))
                .body("veterinarian.name", Matchers.is("Dolittle"))
                .body("veterinarian.title", Matchers.nullValue());
    }

    @Test
    public void testRecordEcho() {
        String response = RestAssured
                .with()
                .body("{\"code\":\"AL\",\"is_enabled\":true,\"name\":\"Alabama\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/record-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("Alabama"))
                .body("code", Matchers.is("AL"))
                .body("is_enabled", Matchers.is(true))
                .extract()
                .asString();

        int first = response.indexOf("is_enabled");
        int last = response.lastIndexOf("is_enabled");
        // assert that the "is_enabled" field is present only once in the response
        assertTrue(first >= 0);
        assertEquals(first, last);
    }

    @Test
    public void testRecordWithEmptyConstructorEcho() {
        RestAssured
                .with()
                .body("{\"name\":\"Bart\",\"age\":5}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/empty-ctor-record-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("Bart"))
                .body("age", Matchers.is(5));
    }

    @Test
    public void testKotlinDataEcho() {
        RestAssured
                .with()
                .body("{\"access_token\":\"ABC\",\"expires_in\":3600}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/kotlin-data-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("access_token", Matchers.is("ABC"))
                .body("expires_in", Matchers.is(3600))
                .extract()
                .asString();
    }

    @Test
    public void testNullMapEcho() {
        RestAssured
                .with()
                .body(new MapWrapper("test"))
                .contentType("application/json; charset=utf-8")
                .post("/simple/null-map-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.is("test"))
                .body("properties", Matchers.nullValue());
    }

    @Test
    public void testItem() {
        RestAssured
                .with()
                .get("/simple/item")
                .then()
                .statusCode(200)
                .body("name", Matchers.is("Name"))
                .body("email", Matchers.is("E-mail"));
    }

    @Test
    public void testItemExtended() {
        RestAssured
                .with()
                .get("/simple/item-extended")
                .then()
                .statusCode(200)
                .body("name", Matchers.is("Name"))
                .body("email", Matchers.is("E-mail"))
                .body("nameExtended", Matchers.is("Name-Extended"))
                .body("emailExtended", Matchers.is(emptyOrNullString()));
    }

    @Test
    void testJsonValuePublicMethod() {
        RestAssured.given()
                .queryParam("value", 240)
                .post("/simple/json-value-public-method")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("\"" + Integer.toString(240) + "\""));
    }

    @Test
    void testJsonValuePublicField() {
        RestAssured.given()
                .queryParam("value", 368)
                .post("/simple/json-value-public-field")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(Integer.toString(368)));
    }

    @Test
    void testJsonValuePrivateMethod() {
        RestAssured.given()
                .queryParam("value", 256)
                .post("/simple/json-value-private-method")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("\"" + Integer.toString(256) + "\""));
    }

    @Test
    void testJsonValuePrivateField() {
        RestAssured.given()
                .queryParam("value", 795)
                .post("/simple/json-value-private-field")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(Integer.toString(795)));
    }

    @Test
    public void testPojoWithJsonCreator() {
        RestAssured
                .with()
                .body("{\"author\":\"Stephen King\",\"title\":\"IT\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/book-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("author", Matchers.is("Stephen King"))
                .body("title", Matchers.is("IT"));
    }

    @Test
    public void testPojoWithFluentSetters() {
        RestAssured
                .with()
                .body("{\"author\":\"Mario Fusco\",\"title\":\"Lombok must die\"}")
                .contentType("application/json; charset=utf-8")
                .post("/simple/lombok-book-echo")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("author", Matchers.is("Mario Fusco"))
                .body("title", Matchers.is("Lombok must die"));
    }

    @Test
    public void testPrimitiveTypesBean() {
        RestAssured
                .with()
                .body("""
                        {
                        "charPrimitive":"b",
                        "characterPrimitive":"c",
                        "shortPrimitive":4,
                        "shortInstance":5,
                        "intPrimitive":6,
                        "integerInstance":7,
                        "longPrimitive":8,
                        "longInstance":9,
                        "floatPrimitive":10.3,
                        "floatInstance":11.4,
                        "doublePrimitive":12.5,
                        "doubleInstance":13.6,
                        "booleanPrimitive":true,
                        "booleanInstance":false
                        }
                        """)
                .contentType("application/json; charset=utf-8")
                .post("/simple/primitive-types-bean")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("charPrimitive", Matchers.is("b"))
                .body("characterPrimitive", Matchers.is("c"))
                .body("shortPrimitive", Matchers.equalTo(4))
                .body("shortInstance", Matchers.equalTo(5))
                .body("intPrimitive", Matchers.equalTo(6))
                .body("integerInstance", Matchers.equalTo(7))
                .body("longPrimitive", Matchers.equalTo(8))
                .body("longInstance", Matchers.equalTo(9))
                .body("floatPrimitive", Matchers.equalTo(10.3F))
                .body("floatInstance", Matchers.equalTo(11.4F))
                .body("doublePrimitive", Matchers.equalTo(12.5F))
                .body("doubleInstance", Matchers.equalTo(13.6F))
                .body("booleanPrimitive", Matchers.equalTo(true))
                .body("booleanInstance", Matchers.equalTo(false));

        // Note: characters are handled weirdly on the Jackson side, we cannot fully test them.
        RestAssured
                .with()
                .body("""
                        {
                        "characterPrimitive":"c"
                        }
                        """)
                .contentType("application/json; charset=utf-8")
                .post("/simple/primitive-types-bean")
                .then()
                .statusCode(200)
                .contentType("application/json")
                //.body("charPrimitive", Matchers.is(""))
                .body("characterPrimitive", Matchers.is("c"))
                .body("shortPrimitive", Matchers.equalTo(0))
                .body("shortInstance", Matchers.nullValue())
                .body("intPrimitive", Matchers.equalTo(0))
                .body("integerInstance", nullValue())
                .body("longPrimitive", Matchers.equalTo(0))
                .body("longInstance", nullValue())
                .body("floatPrimitive", Matchers.equalTo(0F))
                .body("floatInstance", nullValue())
                .body("doublePrimitive", Matchers.equalTo(0F))
                .body("doubleInstance", Matchers.nullValue())
                .body("booleanPrimitive", Matchers.equalTo(false))
                .body("booleanInstance", Matchers.nullValue());
    }

    @Test
    public void testPrimitiveTypesRecord() {
        RestAssured
                .with()
                .body("""
                        {
                        "charPrimitive":"b",
                        "characterPrimitive":"c",
                        "shortPrimitive":4,
                        "shortInstance":5,
                        "intPrimitive":6,
                        "integerInstance":7,
                        "longPrimitive":8,
                        "longInstance":9,
                        "floatPrimitive":10.3,
                        "floatInstance":11.4,
                        "doublePrimitive":12.5,
                        "doubleInstance":13.6,
                        "booleanPrimitive":true,
                        "booleanInstance":false
                        }
                        """)
                .contentType("application/json; charset=utf-8")
                .post("/simple/primitive-types-record")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("charPrimitive", Matchers.is("b"))
                .body("characterPrimitive", Matchers.is("c"))
                .body("shortPrimitive", Matchers.equalTo(4))
                .body("shortInstance", Matchers.equalTo(5))
                .body("intPrimitive", Matchers.equalTo(6))
                .body("integerInstance", Matchers.equalTo(7))
                .body("longPrimitive", Matchers.equalTo(8))
                .body("longInstance", Matchers.equalTo(9))
                .body("floatPrimitive", Matchers.equalTo(10.3F))
                .body("floatInstance", Matchers.equalTo(11.4F))
                .body("doublePrimitive", Matchers.equalTo(12.5F))
                .body("doubleInstance", Matchers.equalTo(13.6F))
                .body("booleanPrimitive", Matchers.equalTo(true))
                .body("booleanInstance", Matchers.equalTo(false));

        // Note: characters are handled weirdly on the Jackson side, we cannot fully test them.
        RestAssured
                .with()
                .body("""
                        {
                        "characterPrimitive":"c"
                        }
                        """)
                .contentType("application/json; charset=utf-8")
                .post("/simple/primitive-types-record")
                .then()
                .statusCode(200)
                .contentType("application/json")
                //.body("charPrimitive", Matchers.is(""))
                .body("characterPrimitive", Matchers.is("c"))
                .body("shortPrimitive", Matchers.equalTo(0))
                .body("shortInstance", Matchers.nullValue())
                .body("intPrimitive", Matchers.equalTo(0))
                .body("integerInstance", nullValue())
                .body("longPrimitive", Matchers.equalTo(0))
                .body("longInstance", nullValue())
                .body("floatPrimitive", Matchers.equalTo(0F))
                .body("floatInstance", nullValue())
                .body("doublePrimitive", Matchers.equalTo(0F))
                .body("doubleInstance", Matchers.nullValue())
                .body("booleanPrimitive", Matchers.equalTo(false))
                .body("booleanInstance", Matchers.nullValue());
    }
}
