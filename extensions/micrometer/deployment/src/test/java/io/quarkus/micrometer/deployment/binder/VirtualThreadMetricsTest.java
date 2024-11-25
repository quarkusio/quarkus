package io.quarkus.micrometer.deployment.binder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.runtime.binder.virtualthreads.VirtualThreadCollector;
import io.quarkus.test.QuarkusUnitTest;

@EnabledForJreRange(min = JRE.JAVA_21)
public class VirtualThreadMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withEmptyApplication();

    @Inject
    Instance<VirtualThreadCollector> collector;

    @Test
    void testInstancePresent() {
        assertTrue(collector.isResolvable(), "VirtualThreadCollector expected");
    }

    @Test
    void testBinderCreated() {
        assertThat(collector.get().getBinder()).isNotNull();
    }

    @Test
    void testTags() {
        assertThat(collector.get().getTags()).isEmpty();
    }

}
