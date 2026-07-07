package io.quarkus.gradle.application.dsl;

import org.gradle.api.Action;

/**
 * A named Quarkus application output whose package has a configurable JAR manifest.
 */
public interface QuarkusApplicationJarOutput {

    /**
     * Returns this output's provider-backed manifest configuration.
     *
     * @return the manifest configuration
     */
    QuarkusApplicationManifest getManifest();

    /**
     * Configures this output's manifest.
     *
     * @param action the manifest configuration action
     */
    default void manifest(Action<? super QuarkusApplicationManifest> action) {
        action.execute(getManifest());
    }
}
