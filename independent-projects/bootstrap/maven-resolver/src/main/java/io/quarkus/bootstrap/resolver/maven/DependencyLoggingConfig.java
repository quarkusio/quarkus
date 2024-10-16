package io.quarkus.bootstrap.resolver.maven;

import java.util.function.Consumer;

public class DependencyLoggingConfig {

    public static Builder builder() {
        return new DependencyLoggingConfig().new Builder();
    }

    public class Builder {

        private boolean built;

        private Builder() {
        }

        public Builder setGraph(boolean graph) {
            if (!built) {
                DependencyLoggingConfig.this.graph = graph;
            }
            return this;
        }

        public Builder setVerbose(boolean verbose) {
            if (!built) {
                DependencyLoggingConfig.this.verbose = verbose;
            }
            return this;
        }

        public Builder setMessageConsumer(Consumer<String> msgConsumer) {
            if (!built) {
                DependencyLoggingConfig.this.msgConsumer = msgConsumer;
            }
            return this;
        }

        public DependencyLoggingConfig build() {
            if (!built) {
                built = true;
                if (msgConsumer == null) {
                    throw new IllegalArgumentException("msgConsumer has not been initialized");
                }
            }
            return DependencyLoggingConfig.this;
        }
    }

    private boolean verbose;
    private boolean graph;
    private Consumer<String> msgConsumer;

    public boolean isGraph() {
        return graph;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public Consumer<String> getMessageConsumer() {
        return msgConsumer;
    }
}
