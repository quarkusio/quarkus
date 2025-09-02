package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.component.beans.Delta;
import io.quarkus.test.junit.mockito.InjectSpy;

public class InjectSpyFailureTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = QuarkusComponentTestExtension.builder()
            .addComponentClasses(Delta.class)
            .buildShouldFail()
            .build();

    @InjectSpy
    Delta delta;

    @Test
    public void testStartFailure() {
        Throwable failure = extension.getBuildFailure();
        assertTrue(failure instanceof IllegalStateException);
    }

}
