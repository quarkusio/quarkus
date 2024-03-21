package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VCSUriConfig {

    /**
     * Whether the vcs-uri annotation should be added to the generated configuration.
     */
    @ConfigItem(defaultValue = "true")
    boolean enabled;

    /**
     * Optional override of the vcs-uri annotation.
     */
    @ConfigItem
    Optional<String> override;
}
