package io.quarkus.vertx.web.params;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;

public class VoidInvalidParameterTest {

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

        @Route
        void hello(@Param("myParam") String param) {
            // This route is illegal - it's not possible to end the response
        }

    }

}
