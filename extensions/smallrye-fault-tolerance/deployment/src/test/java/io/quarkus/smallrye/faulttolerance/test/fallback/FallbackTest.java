package io.quarkus.smallrye.faulttolerance.test.fallback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FallbackTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(FallbackBean.class));

    @Inject
    FallbackBean fallback;

    @Test
    public void test() {
        assertEquals(FallbackBean.RecoverFallback.class.getName(), fallback.hello());
    }
}
