package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.faulttolerance.api.RateLimitException;

public class RateLimitConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(RateLimitConfigBean.class))
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RateLimitConfigBean/value\".rate-limit.value",
                    "3")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RateLimitConfigBean/window\".rate-limit.window",
                    "100")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RateLimitConfigBean/window\".rate-limit.window-unit",
                    "millis")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RateLimitConfigBean/minSpacing\".rate-limit.min-spacing",
                    "100")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.RateLimitConfigBean/minSpacing\".rate-limit.min-spacing-unit",
                    "millis");

    @Inject
    private RateLimitConfigBean bean;

    @Test
    public void value() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(bean.value()).isEqualTo("value");
        }
        assertThatThrownBy(() -> bean.value()).isExactlyInstanceOf(RateLimitException.class);
    }

    @Test
    public void window() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(bean.window()).isEqualTo("window");
        }
        assertThatThrownBy(() -> bean.window()).isExactlyInstanceOf(RateLimitException.class);

        Thread.sleep(500);

        assertThat(bean.window()).isEqualTo("window");
    }

    @Test
    public void minSpacing() throws Exception {
        assertThat(bean.minSpacing()).isEqualTo("minSpacing");
        assertThatThrownBy(() -> bean.minSpacing()).isExactlyInstanceOf(RateLimitException.class);

        Thread.sleep(500);

        assertThat(bean.minSpacing()).isEqualTo("minSpacing");
    }
}
