package io.quarkus.vertx.web.mutiny;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class RawUniTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class))
            .setExpectedException(IllegalStateException.class);

    @Test
    public void testValidationFailed() {
        fail();
    }

    static class SimpleBean {

        @SuppressWarnings("rawtypes")
        @Route(path = "hello")
        Uni hello(RoutingContext context) {
            return Uni.createFrom().item("Hello world!");
        }
    }

}
