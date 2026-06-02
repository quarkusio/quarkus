package io.quarkus.observability.runtime;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ObservabilityJsonRPCService {

    @ConfigProperty(name = "otel-collector.url")
    String otelEndpoint;

    @ConfigProperty(name = "grafana.endpoint")
    String grafanaEndpoint;

    public String getGrafanaEndpoint() {
        return Strings.CS.prependIfMissing(grafanaEndpoint, "http://");
    }

    public String getOtelEndpoint() {
        return StringUtils
                .substringAfterLast(otelEndpoint, ":");
    }
}
