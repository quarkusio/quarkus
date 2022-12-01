package io.quarkus.it.resteasy.jackson;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(StaticContentWithTestProfileTest.DummyProfile.class)
public class StaticContentWithTestProfileTest {

    @Test
    public void testIndexHtml() throws Exception {
        when().get("/index.html").then().statusCode(200).body(containsString("<title>Testing Guide</title>"));
    }

    public static class DummyProfile implements QuarkusTestProfile {

    }
}
