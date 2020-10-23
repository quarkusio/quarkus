
package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ResourcesConfig {

    /**
     * Limits Requirements
     */
    ResourcesRequirementsConfig limits;

    /**
     * Requests Requirements
     */
    ResourcesRequirementsConfig requests;

    @ConfigGroup
    public static class ResourcesRequirementsConfig {

        /**
         * CPU Requirements
         */
        @ConfigItem
        Optional<String> cpu;

        /**
         * Memory Requirements
         */
        @ConfigItem
        Optional<String> memory;
    }
}
