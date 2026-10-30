package io.quarkus.observability.runtime;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ObservabilityJsonRPCService {

    @ConfigProperty(name = "otel-collector.url")
    Optional<String> otelEndpoint;

    @ConfigProperty(name = "grafana.endpoint")
    Optional<String> grafanaEndpoint;

    public String getGrafanaEndpoint() {
        return grafanaEndpoint
                .map(e -> Strings.CS.prependIfMissing(e, "http://"))
                .orElse("");
    }

    public String getOtelEndpoint() {
        return otelEndpoint
                .map(e -> StringUtils.substringAfterLast(e, ":"))
                .orElse("");
    }
}
