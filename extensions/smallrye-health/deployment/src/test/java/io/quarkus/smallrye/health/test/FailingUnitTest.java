package io.quarkus.smallrye.health.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class FailingUnitTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FailingHealthCheck.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));
    @Inject
    @Any
    Instance<HealthCheck> checks;

    @Test
    public void testHealthServlet() {
        RestAssured.when().get("/q/health/live").then().statusCode(503);
        RestAssured.when().get("/q/health/ready").then().statusCode(503);
        RestAssured.when().get("/q/health/started").then().statusCode(503);
        RestAssured.when().get("/q/health").then().statusCode(503);
    }

    @Test
    public void testHealthBeans() {
        List<HealthCheck> check = new ArrayList<>();
        for (HealthCheck i : checks) {
            check.add(i);
        }
        assertEquals(1, check.size());
        assertEquals(HealthCheckResponse.Status.DOWN, check.get(0).call().getStatus());
    }
}
