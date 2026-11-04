package io.quarkus.load.shedding.runtime;

import java.time.Duration;

public final class TestConfig implements LoadSheddingRuntimeConfig {
    private final boolean enabled;
    private final int minLimit;
    private final int maxLimit;
    private final int alphaFactor;
    private final int betaFactor;
    private final double probeFactor;
    private final int initialLimit;
    private final Duration maxRetryAfter;
    private final boolean priorityEnabled;

    public TestConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.minLimit = builder.minLimit;
        this.maxLimit = builder.maxLimit;
        this.alphaFactor = builder.alphaFactor;
        this.betaFactor = builder.betaFactor;
        this.probeFactor = builder.probeFactor;
        this.initialLimit = builder.initialLimit;
        this.maxRetryAfter = builder.maxRetryAfter;
        this.priorityEnabled = builder.priorityEnabled;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public int minLimit() {
        return minLimit;
    }

    @Override
    public int maxLimit() {
        return maxLimit;
    }

    @Override
    public int alphaFactor() {
        return alphaFactor;
    }

    @Override
    public int betaFactor() {
        return betaFactor;
    }

    @Override
    public double probeFactor() {
        return probeFactor;
    }

    @Override
    public int initialLimit() {
        return initialLimit;
    }

    @Override
    public Duration maxRetryAfter() {
        return maxRetryAfter;
    }

    @Override
    public PriorityLoadShedding priority() {
        return new PriorityLoadShedding() {
            @Override
            public boolean enabled() {
                return priorityEnabled;
            }
        };
    }

    public static final class Builder {
        // defaults follow `LoadSheddingRuntimeConfig`
        private boolean enabled = true;
        private int minLimit = 20;
        private int maxLimit = 1000;
        private int alphaFactor = 3;
        private int betaFactor = 6;
        private double probeFactor = 30.0;
        private int initialLimit = 100;
        private Duration maxRetryAfter = Duration.ofSeconds(120);
        private boolean priorityEnabled = true;

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withMinLimit(int minLimit) {
            this.minLimit = minLimit;
            return this;
        }

        public Builder withMaxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
            return this;
        }

        public Builder withAlphaFactor(int alphaFactor) {
            this.alphaFactor = alphaFactor;
            return this;
        }

        public Builder withBetaFactor(int betaFactor) {
            this.betaFactor = betaFactor;
            return this;
        }

        public Builder withProbeFactor(double probeFactor) {
            this.probeFactor = probeFactor;
            return this;
        }

        public Builder withInitialLimit(int initialLimit) {
            this.initialLimit = initialLimit;
            return this;
        }

        public Builder withPriorityEnabled(boolean priorityEnabled) {
            this.priorityEnabled = priorityEnabled;
            return this;
        }

        public Builder withMaxRetryAfter(Duration maxRetryAfter) {
            this.maxRetryAfter = maxRetryAfter;
            return this;
        }

        public TestConfig build() {
            return new TestConfig(this);
        }
    }
}
