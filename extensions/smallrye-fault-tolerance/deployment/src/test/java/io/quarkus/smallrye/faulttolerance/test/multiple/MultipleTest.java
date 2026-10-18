package io.quarkus.smallrye.faulttolerance.test.multiple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MultipleTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(MultipleBean.class));

    @Inject
    MultipleBean bean;

    @Test
    public void test() {
        assertEquals("fallback", bean.hello());
    }
}
