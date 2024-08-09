package io.quarkus.observability.testcontainers;

import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractGrafanaConfig;
import io.quarkus.observability.common.config.LgtmConfig;

public class LgtmContainer extends GrafanaContainer<LgtmContainer, LgtmConfig> {
    /**
     * Logger which will be used to capture container STDOUT and STDERR.
     */
    private static final Logger log = Logger.getLogger(LgtmContainer.class);

    protected static final String LGTM_NETWORK_ALIAS = "ltgm.testcontainer.docker";

    public LgtmContainer() {
        this(new LgtmConfigImpl());
    }

    public LgtmContainer(LgtmConfig config) {
        super(config);
        addExposedPorts(config.otlpPort());
        withLogConsumer(new LgtmContainerLogConsumer(log).withPrefix("LGTM"));
    }

    public int getOtlpPort() {
        return getMappedPort(config.otlpPort());
    }

    protected static class LgtmConfigImpl extends AbstractGrafanaConfig implements LgtmConfig {
        public LgtmConfigImpl() {
            this(ContainerConstants.LGTM);
        }

        public LgtmConfigImpl(String imageName) {
            super(imageName);
        }

        @Override
        public Optional<Set<String>> networkAliases() {
            return Optional.of(Set.of("lgtm", LGTM_NETWORK_ALIAS));
        }

        @Override
        public int otlpPort() {
            return ContainerConstants.OTEL_HTTP_EXPORTER_PORT;
        }
    }
}
