package io.quarkus.spring.security.deployment;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.access.prepost.PreFilter;

import io.quarkus.test.QuarkusExtensionTest;

public class UnsupportedPreFilterAnnotationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanWithPreFilter.class, PreFilter.class))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void shouldNotBeCalled() {
        Assertions.fail();
    }

    @ApplicationScoped
    static class BeanWithPreFilter {

        @PreFilter("filterObject.length() > 3")
        public List<String> filterData(List<String> data) {
            return data;
        }
    }
}