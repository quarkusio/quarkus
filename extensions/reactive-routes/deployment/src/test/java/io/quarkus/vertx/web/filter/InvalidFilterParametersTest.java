package io.quarkus.vertx.web.filter;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.core.http.HttpServerRequest;

public class InvalidFilterParametersTest {

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

        @RouteFilter
        void filter(HttpServerRequest req) {
        }

    }

}
