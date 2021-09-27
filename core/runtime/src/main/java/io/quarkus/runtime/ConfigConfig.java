package io.quarkus.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * We don't really use this, because these are configurations for the config itself, so it causes a chicken / egg
 * problem, but we have it so the configurations can be properly documented.
 *
 * Relocation of the Config configurations to the Quarkus namespace is done in
 * {@link io.quarkus.runtime.configuration.ConfigUtils#configBuilder}.
 */
@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class ConfigConfig {
    /**
     * Additional config locations to be loaded with the Config. The configuration support multiple locations
     * separated by a comma and each must represent a valid {@link java.net.URI}.
     */
    @ConfigItem(name = "config.locations")
    public Optional<List<String>> locations;

    /**
     * Accepts a single configuration profile name. If a configuration property cannot be found in the current active
     * profile, the config performs the same lookup in the profile set by this configuration.
     */
    @ConfigItem(name = "config.profile.parent")
    public Optional<String> profileParent;

    /**
     * A property that allows accessing a generated UUID.
     * It generates that UUID at startup time. So it changes between two starts including in dev mode.
     *
     * Access this generated UUID using expressions: `${quarkus.uuid}`.
     */
    @ConfigItem(name = "uuid")
    public Optional<String> uuid;
}
