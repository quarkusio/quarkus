package io.quarkus.arc.processor;

import java.util.Set;

import jakarta.enterprise.inject.Stereotype;

import org.jboss.jandex.DotName;

/**
 * Makes it possible to turn an annotation into a stereotype without adding a {@link Stereotype} annotation to it.
 */
public interface StereotypeRegistrar extends BuildExtension {

    /**
     * Returns a set of annotation types (their names) that should be treated as stereotypes.
     * To modify (meta-)annotations on these annotations, use {@link AnnotationsTransformer}.
     */
    Set<DotName> getAdditionalStereotypes();
}
