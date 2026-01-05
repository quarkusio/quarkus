package io.quarkus.it.jpa.mariadb;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class OfflineTest {

    @Test
    public void testJPAFunctionalityFromServlet() {
        RestAssured.when().get("/offline/dialect").then().body(
                containsString("bytesPerCharacter=1"),
                containsString("noBackslashEscapes=true"));
    }
}
