package io.quarkus.resteasy.reactive.jsonb.deployment.test;

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
                            .addClasses(Person.class, SimpleJsonResource.class);
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
}
