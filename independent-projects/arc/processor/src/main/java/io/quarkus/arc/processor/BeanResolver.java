package io.quarkus.arc.processor;

import java.util.Set;
import javax.enterprise.inject.AmbiguousResolutionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;

/**
 * Implements type-safe resolution rules.
 */
public interface BeanResolver {

    /**
     * Note that this method does not attempt to resolve the ambiguity.
     * 
     * @param requiredType
     * @param requiredQualifiers
     * @return the set of beans which have the given required type and qualifiers
     * @see #resolveAmbiguity(Set)
     */
    Set<BeanInfo> resolveBeans(Type requiredType, AnnotationInstance... requiredQualifiers);

    /**
     * Apply the ambiguous dependency resolution rules.
     * 
     * @param beans
     * @return the resolved bean, or null
     * @throws AmbiguousResolutionException
     * @see {@link #resolveBeans(Type, AnnotationInstance...)}
     */
    BeanInfo resolveAmbiguity(Set<BeanInfo> beans);

}
