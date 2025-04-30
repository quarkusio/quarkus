package io.quarkus.spring.web.resteasy.reactive.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.ws.rs.QueryParam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.quarkus.test.QuarkusProdModeTest;

public class UnbuildableSpringRestControllerInterfaceTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(UnbuildableControllerInterface.class))
            .setApplicationName("unbuildable-rest-controller-interface")
            .setApplicationVersion("0.1-SNAPSHOT")
            .assertBuildException(throwable -> {
                assertThat(throwable).isInstanceOf(RuntimeException.class);
                assertThat(throwable).hasMessageContaining(
                        "Cannot have more than one of @PathParam, @QueryParam, @HeaderParam, @FormParam, @CookieParam, @BeanParam, @Context on method");
            });

    @Test
    public void testBuildLogs() {
        fail("Should not be called");
    }

    @RestController
    @RequestMapping("/unbuildable")
    public interface UnbuildableControllerInterface {
        @GetMapping("/ping")
        String ping();

        @PostMapping("/hello")
        String hello(@RequestParam(required = false) @QueryParam("dung") String params);

    }

}
