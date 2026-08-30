package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.model.ObjectFactory;

/**
 * Registers named sections for one Quarkus JAR manifest.
 * <p>
 * Registration is lazy and section names must be unique within the manifest.
 */
public class QuarkusApplicationManifestSections {

    private final NamedDomainObjectContainer<QuarkusApplicationManifestSection> container;

    /**
     * Creates an empty named-section container.
     *
     * @param objects Gradle's object factory
     */
    @Inject
    public QuarkusApplicationManifestSections(ObjectFactory objects) {
        container = objects.domainObjectContainer(QuarkusApplicationManifestSection.class,
                name -> objects.newInstance(QuarkusApplicationManifestSection.class, name));
    }

    /**
     * Lazily registers an empty named manifest section.
     *
     * @param name the manifest section name
     * @return a provider for the registered section
     */
    public NamedDomainObjectProvider<QuarkusApplicationManifestSection> section(String name) {
        return container.register(name);
    }

    /**
     * Lazily registers and configures a named manifest section.
     *
     * @param name the manifest section name
     * @param action the section configuration action
     * @return a provider for the registered section
     */
    public NamedDomainObjectProvider<QuarkusApplicationManifestSection> section(String name,
            Action<? super QuarkusApplicationManifestSection> action) {
        return container.register(name, action);
    }

    /**
     * Configures every present and future named section.
     *
     * @param action the section configuration action
     */
    public void all(Action<? super QuarkusApplicationManifestSection> action) {
        container.all(action);
    }
}
