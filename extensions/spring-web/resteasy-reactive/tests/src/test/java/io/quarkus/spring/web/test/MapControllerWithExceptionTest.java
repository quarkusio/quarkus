package io.quarkus.spring.web.test;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.quarkus.test.QuarkusUnitTest;

public class MapControllerWithExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MapControllerWithExceptionTest.MapControllers.class))
            .assertException(throwable -> {
                Assertions.assertThat(throwable.getCause().getCause().getMessage())
                        .contains(
                                "Could not create converter for java.util.Map for method java.lang.String ok(java.util.Map<java.lang.String, "
                                        +
                                        "java.lang.String> queryParams) on class");
            });

    @Test
    void failure() {
    }

    @RestController
    @RequestMapping("/another")
    public static class MapControllers {
        @GetMapping("/ok")
        // there is no mapping for @RestQuery with name
        public String ok(@RequestParam(name = "failing") Map<String, String> queryParams) {
            return queryParams.get("framework");
        }
    }
}
