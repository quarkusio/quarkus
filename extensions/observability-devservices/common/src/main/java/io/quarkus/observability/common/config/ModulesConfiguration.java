package io.quarkus.observability.common.config;

import io.quarkus.runtime.annotations.ConfigDocSection;

public interface ModulesConfiguration {
    @ConfigDocSection
    LgtmConfig lgtm();
}
