package io.quarkus.kubernetes.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ServiceAccountConfig {

    /**
     * The name of the service account.
     */
    @ConfigItem
    Optional<String> name;

    /**
     * The namespace of the service account.
     */
    @ConfigItem
    Optional<String> namespace;

    /**
     * Labels of the service account.
     */
    @ConfigItem
    @ConfigDocMapKey("label-name")
    Map<String, String> labels;

    /**
     * If true, this service account will be used in the generated Deployment resource.
     */
    @ConfigItem
    Optional<Boolean> useAsDefault;

    public boolean isUseAsDefault() {
        return useAsDefault.orElse(false);
    }
}
