package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;

public class OverloadDetectorConfigTest {
    @Test
    public void defaultsAreAccepted() {
        assertThatCode(() -> new OverloadDetector(new TestConfig.Builder().build()))
                .doesNotThrowAnyException();
    }

    @Test
    public void nonPositiveMaxLimitIsRejected() {
        assertThatThrownBy(() -> new OverloadDetector(new TestConfig.Builder().withMaxLimit(0).build()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.load-shedding.max-limit");
    }

    @Test
    public void nonPositiveInitialLimitIsRejected() {
        assertThatThrownBy(() -> new OverloadDetector(new TestConfig.Builder().withInitialLimit(0).build()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.load-shedding.initial-limit");
    }

    @Test
    public void initialLimitHigherThanMaxLimitIsRejected() {
        assertThatThrownBy(() -> new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(10)
                .withMaxLimit(5)
                .build()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.load-shedding.initial-limit");
    }

    @Test
    public void nonPositiveAlphaFactorIsRejected() {
        assertThatThrownBy(() -> new OverloadDetector(new TestConfig.Builder().withAlphaFactor(0).build()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.load-shedding.alpha-factor");
    }

    @Test
    public void betaFactorNotGreaterThanAlphaFactorIsRejected() {
        assertThatThrownBy(() -> new OverloadDetector(new TestConfig.Builder()
                .withAlphaFactor(6)
                .withBetaFactor(6)
                .build()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.load-shedding.beta-factor");
    }

    @Test
    public void nonPositiveProbeFactorIsRejected() {
        assertThatThrownBy(() -> new OverloadDetector(new TestConfig.Builder().withProbeFactor(0.0).build()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.load-shedding.probe-factor");
    }

    @Test
    public void notANumberProbeFactorIsRejected() {
        // every comparison against NaN is false, so the probe would simply never happen
        assertThatThrownBy(() -> new OverloadDetector(new TestConfig.Builder().withProbeFactor(Double.NaN).build()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("quarkus.load-shedding.probe-factor");
    }
}
