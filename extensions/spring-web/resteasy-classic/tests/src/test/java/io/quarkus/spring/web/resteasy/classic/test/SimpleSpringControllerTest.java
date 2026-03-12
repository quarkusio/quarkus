package io.quarkus.spring.web.resteasy.classic.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class SimpleSpringControllerTest {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleSpringController.class));

    @Test
    public void testRootResource() {
        when().get("/simple").then().body(is("hello"));
    }

}
