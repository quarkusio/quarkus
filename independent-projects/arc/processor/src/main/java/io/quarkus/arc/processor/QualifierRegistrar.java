package io.quarkus.arc.processor;

import jakarta.inject.Qualifier;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.DotName;

/**
 * Makes it possible to turn an annotation into a qualifier without adding a {@link Qualifier} annotation to it.
 */
public interface QualifierRegistrar extends BuildExtension {

    /**
     * Returns a map of additional qualifers where the key represents the annotation type and the value is an optional set of
     * non-binding members. Here, "non-binding" is meant in the sense of {@code jakarta.enterprise.util.Nonbinding}. I.e.
     * members
     * named in the set will be ignored when the CDI container is selecting a bean instance for a particular injection point.
     */
    Map<DotName, Set<String>> getAdditionalQualifiers();
}
