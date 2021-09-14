package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
                            .addClasses(Person.class, SimpleJsonResource.class, SuperClass.class, DataItem.class, Item.class);
                }
            });

    @Test
    public void testJson() {
        RestAssured.get("/simple/person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .header("transfer-encoding", nullValue())
                .header("content-length", notNullValue())
                .body("first", Matchers.equalTo("Bob"))
                .body("last", Matchers.equalTo("Builder"));

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
