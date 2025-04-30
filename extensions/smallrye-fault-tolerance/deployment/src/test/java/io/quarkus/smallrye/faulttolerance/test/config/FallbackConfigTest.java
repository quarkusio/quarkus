package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FallbackConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(FallbackConfigBean.class, TestConfigExceptionA.class,
                    TestConfigExceptionB.class, FallbackHandlerA.class, FallbackHandlerB.class))
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.FallbackConfigBean/applyOn\".fallback.apply-on",
                    "io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionA")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.FallbackConfigBean/skipOn\".fallback.skip-on",
                    "io.quarkus.smallrye.faulttolerance.test.config.TestConfigExceptionA")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.FallbackConfigBean/fallbackMethod\".fallback.fallback-method",
                    "anotherFallback")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.FallbackConfigBean/fallbackHandler\".fallback.value",
                    "io.quarkus.smallrye.faulttolerance.test.config.FallbackHandlerB");

    @Inject
    private FallbackConfigBean bean;

    @Test
    public void applyOn() {
        assertThat(bean.applyOn()).isEqualTo("FALLBACK");
    }

    @Test
    public void skipOn() {
        assertThatThrownBy(() -> bean.skipOn()).isExactlyInstanceOf(TestConfigExceptionA.class);
    }

    @Test
    public void fallbackMethod() {
        assertThat(bean.fallbackMethod()).isEqualTo("ANOTHER FALLBACK");
    }

    @Test
    public void fallbackHandler() {
        assertThat(bean.fallbackHandler()).isEqualTo("FallbackHandlerB");
    }
}
