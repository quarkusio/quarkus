package io.quarkus.gradle.application.dsl;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;

/**
 * Configures the main attributes and named sections of one named Quarkus JAR output's manifest.
 * <p>
 * The maps are provider-backed and empty by default. Attribute names must be valid
 * {@link java.util.jar.Attributes.Name manifest attribute names}, may not contain double quotes, and must not differ
 * only by case within one section. Quarkus owns final JAR generation; this model therefore does not implement Gradle
 * manifest-file merging. Typed values override conflicting expanded manifest keys supplied through the named build's
 * generic Quarkus build properties.
 */
public abstract class QuarkusApplicationManifest {

    private final QuarkusApplicationManifestSections sections;

    /**
     * Creates an empty manifest configuration.
     *
     * @param objects Gradle's object factory
     */
    @Inject
    public QuarkusApplicationManifest(ObjectFactory objects) {
        sections = objects.newInstance(QuarkusApplicationManifestSections.class, objects);
        getAttributes().convention(Map.of());
    }

    /**
     * Returns attributes for the manifest's main section.
     *
     * @return the lazily configurable main-section attributes
     */
    public abstract MapProperty<String, String> getAttributes();

    /**
     * Returns the named manifest sections.
     *
     * @return the named-section container
     */
    public QuarkusApplicationManifestSections getSections() {
        return sections;
    }

    /**
     * Configures named manifest sections.
     *
     * @param action the section-container configuration action
     */
    public void sections(Action<? super QuarkusApplicationManifestSections> action) {
        action.execute(sections);
    }
}
