package io.quarkus.micrometer.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.runtime.devui.DevUiMetricsSampler;
import io.quarkus.test.QuarkusUnitTest;

public class MetricsSamplerProdGuardTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((JavaArchive jar) -> {
            });

    @Test
    public void samplerBeanIsAbsentInProdMode() {
        assertThat(CDI.current().select(DevUiMetricsSampler.class).isResolvable()).isFalse();
    }
}
