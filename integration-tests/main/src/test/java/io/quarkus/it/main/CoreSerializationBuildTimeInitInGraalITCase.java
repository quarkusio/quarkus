package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class CoreSerializationBuildTimeInitInGraalITCase {

    @Test
    public void testBuildTimeInitOnRegisteredForSerializationClassServlet() throws Exception {
        RestAssured.when().get("/core/serialization-build-time-init").then()
                .body(is("Parent: BUILD_TIME\tChild: BUILD_TIME"));
    }

}
