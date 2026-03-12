package io.quarkus.smallrye.faulttolerance.test.timeout;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TimeoutTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(TimeoutBean.class));

    @Inject
    TimeoutBean timeout;

    @Test
    public void test() {
        assertThrows(TimeoutException.class, () -> timeout.hello());
    }
}
