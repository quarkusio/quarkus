package io.quarkus.smallrye.faulttolerance.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigPropertyGlobalVsClassVsMethodTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ConfigPropertyBean.class))
            .overrideConfigKey("quarkus.fault-tolerance.global.retry.max-retries", "7")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.ConfigPropertyBean\".retry.max-retries",
                    "5")
            .overrideConfigKey(
                    "quarkus.fault-tolerance.\"io.quarkus.smallrye.faulttolerance.test.config.ConfigPropertyBean/triggerException\".retry.max-retries",
                    "6");

    @Inject
    private ConfigPropertyBean bean;

    @Test
    void test() {
        assertThatThrownBy(() -> bean.triggerException()).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(bean.getRetry()).isEqualTo(7);
    }
}
