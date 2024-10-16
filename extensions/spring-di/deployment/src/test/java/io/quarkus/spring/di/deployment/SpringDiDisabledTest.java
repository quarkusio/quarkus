package io.quarkus.spring.di.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.stereotype.Component;

import io.quarkus.test.QuarkusUnitTest;

public class SpringDiDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, Bar.class))
            .setExpectedException(DeploymentException.class)
            .overrideConfigKey("quarkus.spring-di.enabled", "false");

    @Test
    void shouldNotBeCalled() {
        Assertions.fail();
    }

    @Singleton
    public static class Foo {

        @Inject
        Bar bar;
    }

    @Component
    public static class Bar {

    }
}
