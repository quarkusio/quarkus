package io.quarkus.gradle.application.dsl;

import org.gradle.api.provider.SetProperty;

/**
 * Selects ambient configuration keys by prefix and by exact name.
 * <p>
 * A key is captured when either set matches. The owning {@link QuarkusApplicationConfigInputs} block supplies
 * source-specific conventions and wires the selected values as declared Gradle task inputs.
 */
public abstract class QuarkusApplicationConfigInputSet {

    /**
     * Returns the prefixes used to select keys from the owning configuration source.
     *
     * @return the lazily configurable set of prefixes
     */
    public abstract SetProperty<String> getPrefixes();

    /**
     * Returns exact key names selected in addition to prefix matches.
     *
     * @return the lazily configurable set of exact names
     */
    public abstract SetProperty<String> getNames();
}
