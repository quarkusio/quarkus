package io.quarkus.gradle.application.dsl;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Named;
import org.gradle.api.provider.MapProperty;
import org.jspecify.annotations.NonNull;

/**
 * A named section of a named Quarkus JAR output's manifest.
 * <p>
 * The section name is fixed when the element is registered and must be non-blank without double quotes or control
 * characters. Its provider-backed attribute map is empty by default and follows the attribute validation documented by
 * {@link QuarkusApplicationManifest}.
 */
public abstract class QuarkusApplicationManifestSection implements Named {

    private final String name;

    /**
     * Creates a manifest section with the supplied name.
     *
     * @param name the manifest section name
     */
    @Inject
    public QuarkusApplicationManifestSection(String name) {
        this.name = requireNonNull(name, "name");
        getAttributes().convention(Map.of());
    }

    /**
     * Returns the manifest section name.
     *
     * @return the immutable section name
     */
    @Override
    public @NonNull String getName() {
        return name;
    }

    /**
     * Returns the attributes written into this section.
     *
     * @return the lazily configurable section attributes
     */
    public abstract MapProperty<String, String> getAttributes();
}
