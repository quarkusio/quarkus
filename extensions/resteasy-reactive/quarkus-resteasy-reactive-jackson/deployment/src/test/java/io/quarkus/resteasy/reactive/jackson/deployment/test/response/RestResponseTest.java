package io.quarkus.resteasy.reactive.jackson.deployment.test.response;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class RestResponseTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(RestResponseResource.class, JsonSomething.class);
                }
            });

    @Test
    public void test() {
        RestAssured.get("/json")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("{\"firstName\":\"Stef\",\"lastName\":\"Epardaud\"}"))
                .and().contentType("application/json");
        RestAssured.get("/rest-response-json")
                .then().statusCode(200)
                .and().body(Matchers.equalTo("{\"firstName\":\"Stef\",\"lastName\":\"Epardaud\"}"))
                .and().contentType("application/json");
    }
}
