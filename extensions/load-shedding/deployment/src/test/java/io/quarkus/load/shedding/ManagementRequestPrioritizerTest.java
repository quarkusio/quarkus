package io.quarkus.load.shedding;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.load.shedding.runtime.ManagementRequestPrioritizer;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.web.Router;

public class ManagementRequestPrioritizerTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(RouterInit.class));

    @Test
    public void test() {
        String result = when().get("/").then().extract().body().asString();
        assertEquals("false|false|/", result);

        result = when().get("/q/health").then().extract().body().asString();
        assertEquals("false|true|/q/health", result);

        result = when().get("/q/../other").then().extract().body().asString();
        assertEquals("false|false|/other", result);
    }

    @Singleton
    public static class RouterInit {
        public void init(@Observes Router router, ManagementRequestPrioritizer prioritizer) {
            // before `io.quarkus.load.shedding.runtime.HttpLoadShedding`
            router.route().order(-1_000_000_001).handler(ctx -> {
                ctx.end(prioritizer.appliesTo(ctx.request()) // never, wrong type
                        + "|" + prioritizer.appliesTo(ctx) // only if the request targets non-app endpoint
                        + "|" + ctx.normalizedPath());
            });
        }
    }
}
