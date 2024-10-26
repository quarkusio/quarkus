package io.quarkus.kubernetes.deployment;

import java.util.Optional;

public interface ResourcesConfig {
    /**
     * Limits Requirements
     */
    ResourcesRequirementsConfig limits();

    /**
     * Requests Requirements
     */
    ResourcesRequirementsConfig requests();

    interface ResourcesRequirementsConfig {
        /**
         * CPU Requirements
         */
        Optional<String> cpu();

        /**
         * Memory Requirements
         */
        Optional<String> memory();
    }
}
