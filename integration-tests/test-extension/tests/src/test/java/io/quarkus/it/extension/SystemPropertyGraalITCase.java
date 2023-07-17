package io.quarkus.it.extension;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class SystemPropertyGraalITCase {

    @Test
    public void test() {
        when().get("/core/sysprop").then()
                .body(is("test"));
    }
}
