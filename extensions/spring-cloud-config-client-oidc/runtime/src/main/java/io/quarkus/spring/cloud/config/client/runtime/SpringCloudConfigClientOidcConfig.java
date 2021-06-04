package io.quarkus.spring.cloud.config.client.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BOOTSTRAP, name = SpringCloudConfigClientConfig.NAME)
public class SpringCloudConfigClientOidcConfig {
    protected static final String NAME = "spring-cloud-config-oidc";

    /**
     * Name of the OIDC client to use. Use default client by default.
     */
    @ConfigItem
    public Optional<String> oidcClient;

}
