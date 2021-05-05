package io.quarkus.it.resteasy.jackson;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * This and {@link StaticContentWithTestProfileTest} don't really belong in this module,
 * but adding an extra Maven module just to introduce a test that uses a random http port seems
 * like overkill...
 */
@QuarkusTest
public class StaticContentTest {

    @Test
    public void testIndexHtml() {
        when().get("/index.html").then().statusCode(200).body(containsString("<title>Testing Guide</title>"));
    }
}
