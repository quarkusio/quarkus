package io.quarkus.vertx.web.failure;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;

public class ThrowableParamNormalHandlerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class))
            .setExpectedException(IllegalStateException.class);

    @Test
    public void testValidationFailed() {
        fail();
    }

    public static class Routes {

        @Route
        void fail(UnsupportedOperationException e, @Param String type) {
            throw new RuntimeException("Unknown!");
        }

    }

}
