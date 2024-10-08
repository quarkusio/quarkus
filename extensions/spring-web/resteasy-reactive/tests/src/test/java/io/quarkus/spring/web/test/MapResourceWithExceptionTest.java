package io.quarkus.spring.web.test;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.assertj.core.api.Assertions;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

public class MapResourceWithExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MapResourceWithExceptionTest.MapControllers.class))
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

    @Path("/quarkus")
    public static class MapControllers {
        @GET
        public String ok(@RestQuery Map<String, String> queryParams) {
            return queryParams.get("framework");
        }
    }
}
