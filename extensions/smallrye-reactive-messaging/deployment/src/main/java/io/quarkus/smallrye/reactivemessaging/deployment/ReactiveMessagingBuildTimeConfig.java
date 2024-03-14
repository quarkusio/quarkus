package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "messaging", phase = ConfigPhase.BUILD_TIME)
public class ReactiveMessagingBuildTimeConfig {
    /**
     * Whether a health check is published in case the smallrye-health extension is present.
     */
    @ConfigItem(name = "health.enabled", defaultValue = "true")
    public boolean healthEnabled;

    /**
     * Whether it should automatically configure the <em>connector</em> attribute of channels that don't have an
     * upstream source (for incoming channels), or a downstream consumer (for outgoing channels).
     *
     * When enabled, it verifies that there is only a single connector on the classpath. In that case, it automatically
     * associates the <em>orphans</em> channel to the connector, removing the need to add the <code>.connector</code>
     * attribute in the application configuration.
     */
    @ConfigItem(name = "auto-connector-attachment", defaultValue = "true")
    public boolean autoConnectorAttachment;
}
