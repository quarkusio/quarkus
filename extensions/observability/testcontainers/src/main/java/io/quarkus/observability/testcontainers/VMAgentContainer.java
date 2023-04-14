package io.quarkus.observability.testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractContainerConfig;
import io.quarkus.observability.common.config.VMAgentConfig;

public class VMAgentContainer extends ObservabilityContainer<VMAgentContainer, VMAgentConfig> {
    private static final String CONFIG_PATH = "/etc/prometheus/prometheus.yml";

    private static final String CONFIG_YAML = "scrape_configs:\n" +
            "- job_name: observability_metrics\n" +
            "  static_configs:\n" +
            "  - targets:\n" +
            "    - xTARGETx\n" +
            "  scrape_interval: 5s\n";

    private final int port;

    public VMAgentContainer(String vmEndpoint, int scrapePort) {
        this(new VMAgentConfigImpl(), vmEndpoint, scrapePort);
    }

    public VMAgentContainer(VMAgentConfig config, String vmEndpoint, int scrapePort) {
        super(config);
        this.port = scrapePort;
        setCommandParts(new String[] {
                "-promscrape.config=" + CONFIG_PATH,
                "-remoteWrite.url=" + vmEndpoint + "/api/v1/write"
        });
    }

    protected String getConfig() {
        return CONFIG_YAML.replace(
                "xTARGETx",
                String.format("http://%s:%s/q/metrics", GenericContainer.INTERNAL_HOST_HOSTNAME, port));
    }

    @Override
    protected void containerIsCreated(String containerId) {
        super.containerIsCreated(containerId);
        String config = getConfig();
        addFileToContainer(config.getBytes(StandardCharsets.UTF_8), CONFIG_PATH);
    }

    private static class VMAgentConfigImpl extends AbstractContainerConfig implements VMAgentConfig {
        public VMAgentConfigImpl() {
            super(ContainerConstants.VM_AGENT);
        }

        @Override
        public OptionalInt scrapePort() {
            return OptionalInt.empty();
        }
    }

}
