package io.quarkus.arc.test.injection.unsatisfied;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class UnsatisfiedMatchVetoedTypeTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(FooService.class, Consumer.class)
            .shouldFail()
            .build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).rootCause().isInstanceOf(UnsatisfiedResolutionException.class)
                .hasMessageContaining("The following classes match by type, but have been skipped during discovery")
                .hasMessageContaining(
                        "io.quarkus.arc.test.injection.unsatisfied.UnsatisfiedMatchVetoedTypeTest$FooService was annotated with @Vetoed");

    }

    @Singleton
    static class Consumer {

        @Inject
        FooService foo;

    }

    @Vetoed
    @Singleton
    static class FooService {

    }

}
