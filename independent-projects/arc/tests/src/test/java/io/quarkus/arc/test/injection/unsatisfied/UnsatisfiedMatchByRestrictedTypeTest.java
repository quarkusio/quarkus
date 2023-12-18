package io.quarkus.arc.test.injection.unsatisfied;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class UnsatisfiedMatchByRestrictedTypeTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(FooService.class, Consumer.class)
            .shouldFail()
            .build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).rootCause().isInstanceOf(UnsatisfiedResolutionException.class)
                .hasMessageContaining("The following beans match by type excluded by the @Typed annotation")
                .hasMessageContaining(
                        "io.quarkus.arc.test.injection.unsatisfied.UnsatisfiedMatchByRestrictedTypeTest$FooService");
    }

    @Singleton
    static class Consumer {

        @Inject
        FooService foo;

    }

    @Typed(Comparable.class)
    @Singleton
    static class FooService implements Comparable<String> {

        @Override
        public int compareTo(String o) {
            return 0;
        }

    }

}
