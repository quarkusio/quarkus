package io.quarkus.cyclonedx.generator;

import java.util.ArrayList;
import java.util.List;

import org.cyclonedx.model.Dependency;

/**
 * A CycloneDX {@link Dependency} node that, in addition to the inherited {@code dependsOn}
 * relationships, carries a set of {@code provides} relationships (CycloneDX 1.6
 * {@code dependency.provides}).
 * <p>
 * The stock cyclonedx-core-java {@link Dependency} model has no notion of {@code provides},
 * so this subclass keeps the provided bom-refs in a separate list. {@link ProvidesAwareDependencySerializer}
 * recognizes this type when emitting JSON and writes the extra {@code provides} array;
 * the inherited {@code dependsOn} list continues to be handled as usual (so a node may express
 * both relationships). For XML the {@code provides} refs are not emitted, as the XML path is not
 * yet customized.
 */
public class ProvidesDependency extends Dependency {

    private final List<String> provides = new ArrayList<>();

    public ProvidesDependency(String ref) {
        super(ref);
    }

    /**
     * Records that this component provides/implements the component with the given bom-ref.
     * Duplicate refs are ignored to keep the emitted {@code provides} array free of repeats.
     *
     * @param ref the bom-ref of the provided component
     */
    public void addProvides(String ref) {
        if (!provides.contains(ref)) {
            provides.add(ref);
        }
    }

    /**
     * The bom-refs of the components provided/implemented by this component.
     *
     * @return the provided bom-refs, never {@code null}
     */
    public List<String> getProvides() {
        return provides;
    }
}
