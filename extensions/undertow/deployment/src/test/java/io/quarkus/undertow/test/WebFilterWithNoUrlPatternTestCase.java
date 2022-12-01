package io.quarkus.undertow.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WebFilterWithNoUrlPatternTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NoUrlPatternWebFilter.class, NamedServlet.class));

    @Test
    public void testFilterWithNoUrlPattern() {
        when().get("/named").then()
                .statusCode(200)
                .body(is("Goodbye"));
    }
}
