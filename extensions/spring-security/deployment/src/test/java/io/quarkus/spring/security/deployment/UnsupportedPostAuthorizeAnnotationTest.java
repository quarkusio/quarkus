package io.quarkus.spring.security.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.access.prepost.PostAuthorize;

import io.quarkus.test.QuarkusExtensionTest;

public class UnsupportedPostAuthorizeAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithPostAuthorize.class, PostAuthorize.class))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void shouldNotBeCalled() {
        Assertions.fail();
    }

    @ApplicationScoped
    static class BeanWithPostAuthorize {

        @PostAuthorize("returnObject.owner == authentication.name")
        public String getData() {
            return "data";
        }
    }
}