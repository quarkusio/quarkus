package io.quarkus.load.shedding;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.load.shedding.runtime.OverloadDetector;
import io.quarkus.test.QuarkusExtensionTest;

public class OverloadDetectorRequestEndTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyResource.class))
            .overrideConfigKey("quarkus.load-shedding.initial-limit", "5")
            .overrideConfigKey("quarkus.load-shedding.max-limit", "10")
            .overrideConfigKey("quarkus.load-shedding.priority.enabled", "false");

    @Inject
    OverloadDetector detector;

    @Test
    public void requestsEndedWithoutMeasurementLeaveTheLimitAlone() {
        int begun = beginUntilOverloaded();
        assertThat(begun).isEqualTo(5);
        when().get("/").then().statusCode(503);

        for (int i = 0; i < begun; i++) {
            detector.requestEnd();
        }
        assertThat(detector.isOverloaded()).isFalse();
        when().get("/").then().statusCode(200);

        // the limit is what it was: it takes as many requests as before to reach it
        int begunAgain = beginUntilOverloaded();
        assertThat(begunAgain).isEqualTo(begun);
        for (int i = 0; i < begunAgain; i++) {
            detector.requestEnd();
        }
    }

    private int beginUntilOverloaded() {
        int begun = 0;
        while (!detector.isOverloaded()) {
            detector.requestBegin();
            begun++;
        }
        return begun;
    }

    @Path("/")
    public static class MyResource {
        @GET
        public String hello() {
            return "Hello, world!";
        }
    }
}
