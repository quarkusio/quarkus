package io.quarkus.observability.test;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public abstract class LgtmConfigTestBase extends LgtmTestBase {

    @ConfigProperty(name = "grafana.endpoint")
    String endpoint;

    @ConfigProperty(name = "prometheus.endpoint")
    String prometheusEp;

    @ConfigProperty(name = "tempo-mcp.endpoint")
    String tempoEp;

    @Override
    protected String grafanaEndpoint() {
        return endpoint;
    }

    @Override
    protected String prometheusEndpoint() {
        return prometheusEp;
    }

    @Override
    protected String tempoEndpoint() {
        return tempoEp;
    }
}
