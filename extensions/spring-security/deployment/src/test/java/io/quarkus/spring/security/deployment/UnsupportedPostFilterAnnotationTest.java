package io.quarkus.spring.security.deployment;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.access.prepost.PostFilter;

import io.quarkus.test.QuarkusExtensionTest;

public class UnsupportedPostFilterAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithPostFilter.class, PostFilter.class))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void shouldNotBeCalled() {
        Assertions.fail();
    }

    @ApplicationScoped
    static class BeanWithPostFilter {

        @PostFilter("filterObject.length() > 3")
        public List<String> filterResults() {
            return List.of("a", "abcd", "ab", "abcde");
        }
    }
}