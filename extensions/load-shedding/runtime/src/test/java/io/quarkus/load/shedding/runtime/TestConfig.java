package io.quarkus.load.shedding.runtime;

import java.time.Duration;

import io.quarkus.load.shedding.runtime.LoadSheddingRuntimeConfig.PriorityLoadShedding;
import io.smallrye.config.ConfigInstanceBuilder;

public final class TestConfig {
    private TestConfig() {
    }

    public static final class Builder {
        // values not set explicitly use the defaults declared in `LoadSheddingRuntimeConfig`
        private final ConfigInstanceBuilder<LoadSheddingRuntimeConfig> config = ConfigInstanceBuilder
                .forInterface(LoadSheddingRuntimeConfig.class);
        private final ConfigInstanceBuilder<PriorityLoadShedding> priority = ConfigInstanceBuilder
                .forInterface(PriorityLoadShedding.class);

        public Builder withEnabled(boolean enabled) {
            config.with(LoadSheddingRuntimeConfig::enabled, enabled);
            return this;
        }

        public Builder withMinLimit(int minLimit) {
            config.with(LoadSheddingRuntimeConfig::minLimit, minLimit);
            return this;
        }

        public Builder withMaxLimit(int maxLimit) {
            config.with(LoadSheddingRuntimeConfig::maxLimit, maxLimit);
            return this;
        }

        public Builder withAlphaFactor(int alphaFactor) {
            config.with(LoadSheddingRuntimeConfig::alphaFactor, alphaFactor);
            return this;
        }

        public Builder withBetaFactor(int betaFactor) {
            config.with(LoadSheddingRuntimeConfig::betaFactor, betaFactor);
            return this;
        }

        public Builder withProbeFactor(double probeFactor) {
            config.with(LoadSheddingRuntimeConfig::probeFactor, probeFactor);
            return this;
        }

        public Builder withInitialLimit(int initialLimit) {
            config.with(LoadSheddingRuntimeConfig::initialLimit, initialLimit);
            return this;
        }

        public Builder withPriorityEnabled(boolean priorityEnabled) {
            priority.with(PriorityLoadShedding::enabled, priorityEnabled);
            return this;
        }

        public Builder withMaxRetryAfter(Duration maxRetryAfter) {
            config.with(LoadSheddingRuntimeConfig::maxRetryAfter, maxRetryAfter);
            return this;
        }

        public LoadSheddingRuntimeConfig build() {
            return config.with(LoadSheddingRuntimeConfig::priority, priority.build()).build();
        }
    }
}
