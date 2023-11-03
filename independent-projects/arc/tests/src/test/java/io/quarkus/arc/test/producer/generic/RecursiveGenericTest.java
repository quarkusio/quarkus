package io.quarkus.arc.test.producer.generic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class RecursiveGenericTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Producer.class, Target.class)
            .build();

    @Test
    public void test() {
        Target target = Arc.container().instance(Target.class).get();
        assertNotNull(target.list);

        assertNotNull(Arc.container().instance(new TypeLiteral<List<String>>() {
        }).get());
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
        List<String> list;
    }
}
