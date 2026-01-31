package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class UnmockablePrimitiveFailureTest {

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
                "Unsatisfied injection point with required type 'int' cannot be mocked automatically: io.quarkus.test.component.UnmockablePrimitiveFailureTest$MyBean#val"));
    }

    @Singleton
    public static class MyBean {

        @Inject
        int val;

    }

}
