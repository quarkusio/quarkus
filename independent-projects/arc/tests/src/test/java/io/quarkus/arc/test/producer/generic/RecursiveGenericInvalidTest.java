package io.quarkus.arc.test.producer.generic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class RecursiveGenericInvalidTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Producer.class, Target.class)
            .shouldFail()
            .build();

    @Test
    public void test() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().contains("Unsatisfied dependency"),
                container.getFailure().getMessage());
    }

    @Singleton
    static class Producer {
        @Produces
        @Dependent
        <T extends Comparable<T>> List<T> produce() {
            return new ArrayList<>();
        }
    }

    @Singleton
    static class Target {
        @Inject
        List<Object> list; // Object is not Comparable, this injection point can't be resolved
    }
}
