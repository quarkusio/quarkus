package io.quarkus.kubernetes.deployment;

public interface NodeSelectorConfig {
    /**
     * The key of the nodeSelector.
     */
    String key();

    /**
     * The value of the nodeSelector.
     */
    String value();
}
