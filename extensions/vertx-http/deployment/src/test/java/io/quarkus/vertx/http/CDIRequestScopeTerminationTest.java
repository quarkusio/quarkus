package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class CDIRequestScopeTerminationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(CdiRequestScopeRoute.class));

    @Test
    public void testRequestScopeTerminatedOnDuplicatedContext() {
        assertEquals(RestAssured.given().get("/cdi-request-scope").asString(), "false");

    }

    @ApplicationScoped
    static class CdiRequestScopeRoute {

        public void register(@Observes Router router) {
            router.route("/cdi-request-scope").handler(ctx -> {
                Arc.container().requestContext().activate();
                Arc.container().requestContext().terminate();
                ctx.response().end(Arc.container().requestContext().isActive() + "");
            });
        }

    }

}
