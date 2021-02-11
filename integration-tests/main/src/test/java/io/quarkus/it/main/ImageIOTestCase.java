package io.quarkus.it.main;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ImageIOTestCase {

    @Test
    public void testImageRead() {
        when().get("/core/imageio").then().body(equalTo("1x1"));
    }

}
