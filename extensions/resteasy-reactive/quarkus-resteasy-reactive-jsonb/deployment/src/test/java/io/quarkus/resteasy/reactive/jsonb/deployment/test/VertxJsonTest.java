package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class VertxJsonTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(VertxJsonEndpoint.class);
                }
            });

    @Test
    public void testJsonObject() {
        RestAssured.with()
                .body("{\"name\": \"Bob\"}")
                .contentType("application/json")
                .post("/vertx/jsonObject")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("name", Matchers.equalTo("Bob"))
                .body("age", Matchers.equalTo(50))
                .body("nested.foo", Matchers.equalTo("bar"))
                .body("bools[0]", Matchers.equalTo(true));
    }

    @Test
    public void testJsonArray() {
        RestAssured.with()
                .body("[\"first\"]")
                .contentType("application/json")
                .post("/vertx/jsonArray")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("[0]", Matchers.equalTo("first"))
                .body("[1]", Matchers.equalTo("last"));
    }

}
