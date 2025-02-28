package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.messaging")
public interface ReactiveMessagingBuildTimeConfig {
    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();

    /**
     * Whether it should automatically configure the <em>connector</em> attribute of channels that don't have an
     * upstream source (for incoming channels), or a downstream consumer (for outgoing channels).
     *
     * When enabled, it verifies that there is only a single connector on the classpath. In that case, it automatically
     * associates the <em>orphans</em> channel to the connector, removing the need to add the <code>.connector</code>
     * attribute in the application configuration.
     */
    @WithName("auto-connector-attachment")
    @WithDefault("true")
    boolean autoConnectorAttachment();

    /**
     * Whether to enable the RequestScope context on a message context
     */
    @WithName("request-scoped.enabled")
    @WithDefault("false")
    boolean activateRequestScopeEnabled();
}
