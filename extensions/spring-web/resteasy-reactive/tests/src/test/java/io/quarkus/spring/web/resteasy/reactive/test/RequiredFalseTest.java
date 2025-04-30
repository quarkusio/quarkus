package io.quarkus.spring.web.resteasy.reactive.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.quarkus.test.QuarkusUnitTest;

public class RequiredFalseTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Controller.class));

    @Test
    public void test() {
        when().get("/endpoint/boxed")
                .then()
                .statusCode(200)
                .body(is(""));

        when().get("/endpoint/primitive")
                .then()
                .statusCode(200)
                .body(is("0"));
    }

    @RestController
    @RequestMapping("/endpoint")
    public static class Controller {

        @GetMapping("/boxed")
        public ResponseEntity<Long> boxed(@RequestParam(value = "id", required = false) Long id) {
            return ResponseEntity.ok(id);
        }

        @GetMapping("/primitive")
        public ResponseEntity<Long> primitive(@RequestParam(value = "id", required = false) long id) {
            return ResponseEntity.ok(id);
        }
    }
}
