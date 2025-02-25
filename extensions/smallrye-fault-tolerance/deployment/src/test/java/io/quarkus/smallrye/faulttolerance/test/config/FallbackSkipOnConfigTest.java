package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FallbackSkipOnConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(FallbackConfigBean.class, TestConfigExceptionA.class,
                    TestConfigExceptionB.class, FallbackHandlerA.class))
            .overrideConfigKey("quarkus.fault-tolerance.global.fallback.skip-on",
                    "io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionA");

    @Inject
    private FallbackConfigBean bean;

    @Test
    public void skipOn() {
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }
}
