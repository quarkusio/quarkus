package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class HealthCheckContextPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.smallrye-health.context-propagation", "true")
            .withApplicationRoot((jar) -> jar
                    .addClasses(RequestScopedBean.class, ContextualHC.class));

    @Test
    public void testContextPropagatedToHealthChecks() {
        try {
            RestAssured.defaultParser = Parser.JSON;

            String firstResponse = when().get("/q/health").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP"),
                            "checks.name", contains(ContextualHC.class.getName()),
                            "checks.data", is(notNullValue()))
                    .extract().response().jsonPath().getString("checks.data.request-scoped-instance");

            String secondResponse = when().get("/q/health").then()
                    .extract().response().jsonPath().getString("checks.data.request-scoped-instance");

            String thirdResponse = when().get("/q/health").then()
                    .extract().response().jsonPath().getString("checks.data.request-scoped-instance");

            Assertions.assertNotEquals(firstResponse, secondResponse, getMessage("first", "second"));
            Assertions.assertNotEquals(firstResponse, thirdResponse, getMessage("first", "third"));
            Assertions.assertNotEquals(secondResponse, thirdResponse, getMessage("second", "third"));
        } finally {
            RestAssured.reset();
        }
    }

    private String getMessage(String response1, String response2) {
        return String.format("The CDI context should have been propagated to the health check invocations. " +
                "However, %s and %s responses are the same", response1, response2);
    }

    @RequestScoped
    static class RequestScopedBean {

        String uuid;

        @PostConstruct
        public void init() {
            uuid = UUID.randomUUID().toString();
        }

        public String getUuid() {
            return uuid;
        }
    }

    @Liveness
    @ApplicationScoped
    static class ContextualHC implements HealthCheck {

        @Inject
        RequestScopedBean requestScopedBean;

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.named(ContextualHC.class.getName()).up()
                    .withData("request-scoped-instance", requestScopedBean.getUuid())
                    .build();
        }
    }

}
