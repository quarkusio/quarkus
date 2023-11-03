package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomSerializationTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Person.class, CustomSerializationResource.class, User.class, Views.class);
                }
            });

    @Test
    public void testCustomSerialization() {
        // assert that we get a proper response
        // we can't use json-path to assert because the returned string is not proper json as it does not have quotes around the field names
        RestAssured.get("/custom-serialization/person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("Bob"))
                .body(containsString("Builder"));

        // assert with a list of people
        RestAssured
                .with()
                .body("[{\"first\": \"Bob\", \"last\": \"Builder\"}, {\"first\": \"Bob2\", \"last\": \"Builder2\"}]")
                .contentType("application/json; charset=utf-8")
                .post("/custom-serialization/people/list")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("Bob"))
                .body(containsString("Builder"))
                .body(containsString("Bob2"))
                .body(containsString("Builder2"));

        // a new instance should have been created
        int currentCount = CustomSerializationResource.UnquotedFieldsPersonSerialization.count.get();
        RestAssured.get("/custom-serialization/invalid-use-of-custom-serializer")
                .then()
                .statusCode(500);
        assertEquals(currentCount + 1, CustomSerializationResource.UnquotedFieldsPersonSerialization.count.intValue());
    }

    @Test
    public void testCustomDeserialization() {
        // assert that the reader support the unquoted fields (because we have used a custom object reader
        // via `@CustomDeserialization`
        RestAssured.given()
                .body("{first: \"Hello\", last: \"Deserialization\"}")
                .contentType("application/json; charset=utf-8")
                .post("/custom-serialization/person")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("Hello"))
                .body(containsString("Deserialization"));

        // assert that the instances were re-used as we simply invoked methods that should have already created their object readers
        RestAssured.given()
                .body("{first: \"Hello\", last: \"Deserialization\"}")
                .contentType("application/json; charset=utf-8")
                .post("/custom-serialization/person")
                .then()
                .statusCode(200);

        // assert with a list of people
        RestAssured
                .with()
                .body("[{first: \"Bob\", last: \"Builder\"}, {first: \"Bob2\", last: \"Builder2\"}]")
                .contentType("application/json; charset=utf-8")
                .post("/custom-serialization/people/list")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("Bob"))
                .body(containsString("Builder"))
                .body(containsString("Bob2"))
                .body(containsString("Builder2"));
    }
}
