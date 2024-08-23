package io.quarkus.arc.test.producer.generic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class DuplicateRecursiveGenericTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Producer.class, Target.class)
            .additionalClasses(FooBar.class, FooBarImpl.class)
            .build();

    @Test
    public void test() {
        Target target = Arc.container().instance(Target.class).get();
        assertNotNull(target.foobar);

        assertNotNull(Arc.container().instance(new TypeLiteral<FooBar<FooBarImpl, String>>() {
        }).get());
    }

    @Singleton
    static class Producer {
        @Produces
        @Dependent
        <T extends FooBar<T, U>, U extends Comparable<U>> FooBar<T, U> produce() {
            return new FooBar<>() {
            };
        }
    }

    @Singleton
    static class Target {
        @Inject
        FooBar<FooBarImpl, String> foobar;
    }

    interface FooBar<T extends FooBar<?, U>, U extends Comparable<U>> {
    }

    static class FooBarImpl implements FooBar<FooBarImpl, String> {
    }
}
