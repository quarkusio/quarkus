package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;

public interface ServiceConfig {
    /**
     * Custom annotations to add to the Service resource.
     */
    @ConfigDocMapKey("annotation-name")
    Map<String, String> annotations();
}
