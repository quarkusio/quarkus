package io.quarkus.spring.security.deployment;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.access.annotation.Secured;

import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationMixingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
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
