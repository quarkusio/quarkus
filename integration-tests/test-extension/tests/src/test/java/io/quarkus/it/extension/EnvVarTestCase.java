package io.quarkus.it.extension;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EnvVarTestCase {

    @Test
    public void test() {
        when().get("/core/env").then()
                .body(is("foo-50"));
    }
}
