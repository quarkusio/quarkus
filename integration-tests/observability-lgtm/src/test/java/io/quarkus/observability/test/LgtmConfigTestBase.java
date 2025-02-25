package io.quarkus.observability.test;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public abstract class LgtmConfigTestBase extends LgtmTestBase {

    @ConfigProperty(name = "grafana.endpoint")
    String endpoint;

    @Override
    protected String grafanaEndpoint() {
        return endpoint;
    }
}
