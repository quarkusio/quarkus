package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;
import javax.inject.Named;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class HealthCheckDefaultScopeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(NoScopeCheck.class, NoScopeStereotypeWithoutScopeCheck.class, MyStereotype.class));

    @Test
    public void testHealth() {
        // the health check does not set a content type so we need to force the parser
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("/q/health/live").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP", "UP"),
                            "checks.name", containsInAnyOrder("noScope", "noScopeStereotype"));
            when().get("/q/health/live").then()
                    .body("status", is("DOWN"),
                            "checks.status", contains("DOWN", "DOWN"),
                            "checks.name", containsInAnyOrder("noScope", "noScopeStereotype"));
        } finally {
            RestAssured.reset();
        }
    }

    // No scope - @Singleton is used by default
    @Liveness
    static class NoScopeCheck implements HealthCheck {

        volatile int counter = 0;

        @Override
        public HealthCheckResponse call() {
            if (++counter > 1) {
                return HealthCheckResponse.builder().down().name("noScope").build();
            }
            return HealthCheckResponse.builder().up().name("noScope").build();
        }
    }

    // No scope and stereotype without scope - @Singleton is used by default
    @MyStereotype
    @Liveness
    static class NoScopeStereotypeWithoutScopeCheck implements HealthCheck {

        volatile int counter = 0;

        @Override
        public HealthCheckResponse call() {
            if (++counter > 1) {
                return HealthCheckResponse.builder().down().name("noScopeStereotype").build();
            }
            return HealthCheckResponse.builder().up().name("noScopeStereotype").build();
        }
    }

    @Named
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface MyStereotype {
    }

}
