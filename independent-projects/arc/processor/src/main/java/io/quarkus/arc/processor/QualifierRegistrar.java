package io.quarkus.arc.processor;

import java.util.Map;
import java.util.Set;
import javax.inject.Qualifier;
import org.jboss.jandex.DotName;

/**
 * Makes it possible to turn an annotation into a qualifier without adding a {@link Qualifier} annotation to it.
 */
public interface QualifierRegistrar extends BuildExtension {

    /**
     * Returns a map of additional qualifers where the key represents the annotation type and the value is an optional set of
     * non-binding members.
     */
    Map<DotName, Set<String>> getAdditionalQualifiers();
}
