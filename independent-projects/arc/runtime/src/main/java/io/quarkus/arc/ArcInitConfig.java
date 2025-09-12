package io.quarkus.arc;

/**
 * A configuration object used while initializing Arc, see {@link Arc#initialize()} methods.
 * Consolidates all configuration objects needed for Arc to initialize, values are initialized to their defaults.
 *
 */
public final class ArcInitConfig {

    /**
     * Basic instance without any configuration, all values are default
     */
    public static final ArcInitConfig DEFAULT = builder().build();

    /**
     * Obtains a builder for {@link ArcInitConfig}
     *
     * @return new instance of the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private ArcInitConfig(Builder builder) {
        this.currentContextFactory = builder.currentContextFactory;
        this.strictCompatibility = builder.strictCompatibility;
        this.testMode = builder.testMode;
    }

    private final boolean strictCompatibility;
    private final CurrentContextFactory currentContextFactory;
    private final boolean testMode;

    public boolean isStrictCompatibility() {
        return strictCompatibility;
    }

    public CurrentContextFactory getCurrentContextFactory() {
        return currentContextFactory;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public static class Builder {

        private boolean strictCompatibility;
        private CurrentContextFactory currentContextFactory;
        private boolean testMode;

        private Builder() {
        }

        public Builder setStrictCompatibility(boolean strictCompatibility) {
            this.strictCompatibility = strictCompatibility;
            return this;
        }

        public Builder setCurrentContextFactory(CurrentContextFactory currentContextFactory) {
            this.currentContextFactory = currentContextFactory;
            return this;
        }

        public Builder setTestMode(boolean testMode) {
            this.testMode = testMode;
            return this;
        }

        public ArcInitConfig build() {
            return new ArcInitConfig(this);
        }
    }
}
