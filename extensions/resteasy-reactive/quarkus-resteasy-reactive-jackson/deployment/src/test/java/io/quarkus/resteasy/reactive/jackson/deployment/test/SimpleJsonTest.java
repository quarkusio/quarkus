package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SimpleJsonTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Person.class, SimpleJsonResource.class, User.class, Views.class, SuperClass.class,
                                    OtherPersonResource.class, AbstractPersonResource.class, DataItem.class, Item.class,
                                    NoopReaderInterceptor.class);
                }
            });

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
        assertEquals(0, SimpleJsonResource.UnquotedFieldsPersonBiFunction.count.intValue());

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
        assertEquals(1, SimpleJsonResource.UnquotedFieldsPersonBiFunction.count.intValue());

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
        assertEquals(2, SimpleJsonResource.UnquotedFieldsPersonBiFunction.count.intValue());

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
        // assert that the instances were re-used as we simply invoked methods that should have already created their object writters
        assertEquals(2, SimpleJsonResource.UnquotedFieldsPersonBiFunction.count.intValue());

        RestAssured.get("/simple/invalid-use-of-custom-serializer")
                .then()
                .statusCode(500);
        // a new instance should have been created
        assertEquals(3, SimpleJsonResource.UnquotedFieldsPersonBiFunction.count.intValue());
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

    private void doTestSecurePerson(String basePath, final String path) {
        RestAssured.get(basePath + path)
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body(containsString("Bob"))
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
}
