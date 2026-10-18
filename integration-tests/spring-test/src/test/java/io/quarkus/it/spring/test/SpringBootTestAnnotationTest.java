package io.quarkus.it.spring.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringBootTestAnnotationTest {

    @Autowired
    GreetingService greetingService;

    @Test
    public void testCdiInjection() {
        assertNotNull(greetingService);
        assertEquals("hello quarkus", greetingService.greet("quarkus"));
    }

    @Test
    public void testHttpEndpoint() {
        given()
                .queryParam("name", "spring")
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("hello spring"));
    }
}
