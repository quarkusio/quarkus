package io.quarkus.resteasy.test;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ConstructorInjectionResourceTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ConstructorInjectionResource.class, SingletonConstructorInjectionResource.class,
                            Service.class));

    @Test
    public void testConstructorInjectionResource() {
        RestAssured.when().get("/ctor").then().body(Matchers.is("service"));
    }

    @Test
    public void testSingletonConstructorInjectionResource() {
        RestAssured.when().get("/ctor-single").then().body(Matchers.is("service"));
    }
}
