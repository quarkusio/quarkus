package io.quarkus.arc.test.injection.unsatisfied;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.MyQualifier;

public class UnsatisfiedMatchByTypeTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(FooService.class, Consumer.class, MyQualifier.class)
            .shouldFail()
            .build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).rootCause().isInstanceOf(UnsatisfiedResolutionException.class)
                .hasMessageContaining("The following beans match by type, but none has matching qualifiers")
                .hasMessageContaining("io.quarkus.arc.test.injection.unsatisfied.UnsatisfiedMatchByTypeTest$FooService");

    }

    @Singleton
    static class Consumer {

        @Inject
        @MyQualifier
        FooService foo;

    }

    @Singleton
    static class FooService {

    }

}
