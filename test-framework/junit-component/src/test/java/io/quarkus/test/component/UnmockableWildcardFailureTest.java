package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class UnmockableWildcardFailureTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .addComponentClasses(MyBean.class)
            .buildShouldFail()
            .build();

    @Test
    public void testFailure() {
        Throwable failure = extension.getBuildFailure();
        assertTrue(failure instanceof IllegalStateException);
        assertTrue(failure.getMessage().startsWith(
                "Unsatisfied injection point with required type 'java.util.concurrent.Callable<?>' cannot be mocked automatically: io.quarkus.test.component.UnmockableWildcardFailureTest$MyBean#instance"));
    }

    @Singleton
    public static class MyBean {

        // Callable<?> is illegal bean type
        @Inject
        Instance<Callable<?>> instance;

    }

}
