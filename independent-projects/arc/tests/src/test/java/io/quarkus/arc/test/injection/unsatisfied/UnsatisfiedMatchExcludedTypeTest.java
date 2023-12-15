package io.quarkus.arc.test.injection.unsatisfied;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class UnsatisfiedMatchExcludedTypeTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(FooService.class, Consumer.class)
            .excludeType(c -> c.name().equals(DotName.createSimple(FooService.class)))
            .shouldFail()
            .build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).rootCause().isInstanceOf(UnsatisfiedResolutionException.class)
                .hasMessageContaining("The following classes match by type, but have been skipped during discovery")
                .hasMessageContaining(
                        "io.quarkus.arc.test.injection.unsatisfied.UnsatisfiedMatchExcludedTypeTest$FooService was excluded in config");

    }

    @Singleton
    static class Consumer {

        @Inject
        FooService foo;

    }

    @Singleton
    static class FooService {

    }

}
