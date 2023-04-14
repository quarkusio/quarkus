package io.quarkus.observability.testcontainers;

import java.util.Optional;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractContainerConfig;
import io.quarkus.observability.common.config.VictoriaMetricsConfig;

@SuppressWarnings("resource")
public class VictoriaMetricsContainer extends ObservabilityContainer<VictoriaMetricsContainer, VictoriaMetricsConfig> {

    private final int port;

    public VictoriaMetricsContainer() {
        this(new VictoriaMetricsConfigImpl());
    }

    public VictoriaMetricsContainer(VictoriaMetricsConfig config) {
        super(config);
        this.port = config.port();
        withExposedPorts(port);
    }

    public VictoriaMetricsContainer withMappedPort(int port) {
        addFixedExposedPort(port, this.port);
        return this;
    }

    public String getEndpoint(boolean secure) {
        return "http" + (secure ? "s" : "") + "://" + getHost() + ":" + getFirstMappedPort();
    }

    private static class VictoriaMetricsConfigImpl extends AbstractContainerConfig implements VictoriaMetricsConfig {
        public VictoriaMetricsConfigImpl() {
            super(ContainerConstants.VICTORIA_METRICS);
        }

        @Override
        public Optional<Set<String>> networkAliases() {
            return Optional.of(Set.of("victoria-metrics"));
        }

        @Override
        public int port() {
            return 8428;
        }
    }

}
