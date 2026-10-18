package io.quarkus.spring.security.deployment;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.access.annotation.Secured;

import io.quarkus.test.QuarkusExtensionTest;

public class SecurityAnnotationMixingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SomeBean.class))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void shouldNotBeCalled() {
        Assertions.fail();
    }

    @ApplicationScoped
    @PermitAll
    static class SomeBean {

        @Secured("admin")
        public void doSomething() {

        }

    }
}
