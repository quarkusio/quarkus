package io.quarkus.vertx.http.devmode;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.vertx.ext.web.Router;

public class RecoverFromFailedRuntimeStartTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FailedStartupEndpoint.class))
            .setAllowFailedStart(true);

    @Test
    void recoversAfterFixingTheStartupFailure() {
        when().get("/recovered").then().statusCode(500);

        test.modifySourceFile(RecoverFromFailedRuntimeStartTest.class,
                s -> s.replace("boolean fail = true;\n", "boolean fail = false;\n"));

        when().get("/recovered").then().statusCode(200).body(equalTo("recovered"));
    }

    @ApplicationScoped
    public static class FailedStartupEndpoint {

        void addRoute(@Observes Router router) {
            boolean fail = true;
            if (fail) {
                // fails after logging is set up, but before the HTTP server is started
                throw new IllegalStateException("startup failure");
            }
            router.route("/recovered")
                    .produces("text/plain")
                    .handler(rc -> rc.response().end("recovered"));
        }
    }
}
