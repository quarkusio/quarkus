package io.quarkus.load.shedding;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.load.shedding.runtime.HttpLoadShedding;
import io.quarkus.load.shedding.runtime.ManagementRequestPrioritizer;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

public class ManagementRequestPrioritizerTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RouterInit.class));

    @Test
    public void test() {
        String result = when().get("/").then().extract().body().asString();
        assertEquals("false|false|/", result);

        result = when().get("/q/health").then().extract().body().asString();
        assertEquals("false|true|/q/health", result);

        result = when().get("/q/../other").then().extract().body().asString();
        assertEquals("false|false|/other", result);

        // an application endpoint that merely starts with the same characters as the
        // non-application root path is not a non-application endpoint
        result = when().get("/queries").then().extract().body().asString();
        assertEquals("false|false|/queries", result);

        // the root path itself, on the other hand, is
        result = when().get("/q").then().extract().body().asString();
        assertEquals("false|true|/q", result);
    }

    @Singleton
    public static class RouterInit {
        public void init(@Observes Router router, ManagementRequestPrioritizer prioritizer) {
            router.route().order(HttpLoadShedding.PRIORITY - 1).handler(ctx -> {
                ctx.end(prioritizer.appliesTo(ctx.request()) // never, wrong type
                        + "|" + prioritizer.appliesTo(ctx) // only if the request targets non-app endpoint
                        + "|" + ctx.normalizedPath());
            });
        }
    }
}
