package io.quarkus.vertx.web.params;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;

public class InvalidPathParameterNameTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class))
            .setExpectedException(IllegalStateException.class);

    @Test
    public void testValidationFailed() {
        fail();
    }

    static class SimpleBean {

        @Route(path = "/hello/:my-param")
        String hello(@Param("my-param") String param) {
            return param;
        }

    }

}
