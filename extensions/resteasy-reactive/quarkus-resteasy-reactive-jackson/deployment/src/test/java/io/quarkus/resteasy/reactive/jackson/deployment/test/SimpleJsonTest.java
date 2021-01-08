package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
                            .addClasses(Person.class, SimpleJsonResource.class, User.class, Views.class, SuperClass.class);
                }
            });

    @Test
    public void testJson() {
        RestAssured.get("/simple/person")
                .then()
                .statusCode(200)
                .contentType("application/json")
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
                .contentType("application/json")
                .post("/simple/person-validated")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured
                .with()
                .body(postBody)
                .contentType("application/json")
                .post("/simple/person-invalid-result")
                .then()
                .statusCode(500)
                .contentType("application/json");

        RestAssured
                .with()
                .body("{\"first\": \"Bob\"}")
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
}
