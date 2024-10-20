package io.quarkus.spring.web.test;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MapControllerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MapControllerTest.MapControllers.class));

    @Test
    void ok() {
        RestAssured.when().get("/another/ok?framework=quarkus")
                .then()
                .statusCode(200)
                .body(Matchers.is("quarkus"));
    }

    @RestController
    @RequestMapping("/another")
    public static class MapControllers {
        @GetMapping("/ok")
        public String ok(@RequestParam Map<String, String> queryParams) {
            return queryParams.get("framework");
        }
    }
}
