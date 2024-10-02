package io.quarkus.observability.common.config;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.smallrye.config.ConfigMapping;

@ConfigMapping
public interface ModulesConfiguration {

    /**
     * Grafana LGTM configuration
     */
    @ConfigDocSection
    LgtmConfig lgtm();
}
