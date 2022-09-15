package io.quarkus.arc.processor;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import java.util.Collections;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;

/**
 * Implements type-safe resolution rules.
 */
public interface BeanResolver {

    default Set<BeanInfo> resolveBeans(Type requiredType, AnnotationInstance... requiredQualifiers) {
        return resolveBeans(requiredType, requiredQualifiers.length == 0 ? Collections.emptySet() : Set.of(requiredQualifiers));
    }

    /**
     * Note that this method does not attempt to resolve the ambiguity.
     *
     * @param requiredType
     * @param requiredQualifiers
     * @return the set of beans which have the given required type and qualifiers
     * @see #resolveAmbiguity(Set)
     */
    Set<BeanInfo> resolveBeans(Type requiredType, Set<AnnotationInstance> requiredQualifiers);

    /**
     * Apply the ambiguous dependency resolution rules.
     *
     * @param beans
     * @return the resolved bean, or null
     * @throws AmbiguousResolutionException
     * @see {@link #resolveBeans(Type, AnnotationInstance...)}
     */
    BeanInfo resolveAmbiguity(Set<BeanInfo> beans);

    /**
     * Checks if given {@link BeanInfo} has type and qualifiers matching those in provided
     * {@link InjectionPointInfo.TypeAndQualifiers}.
     *
     * @param bean Candidate bean
     * @param typeAndQualifiers Required type and qualifiers
     * @return True if provided {@link BeanInfo} matches given required type and qualifiers, false otherwise
     */
    boolean matches(BeanInfo bean, InjectionPointInfo.TypeAndQualifiers typeAndQualifiers);

    /**
     * Checks if given {@link BeanInfo} has type and qualifiers matching those in provided
     * {@link InjectionPointInfo.TypeAndQualifiers}.
     *
     * @param bean Candidate bean
     * @param requiredType Required bean type
     * @param requiredQualifiers Required qualifiers
     * @return True if provided {@link BeanInfo} matches given required type and qualifiers, false otherwise
     */
    boolean matches(BeanInfo bean, Type requiredType, Set<AnnotationInstance> requiredQualifiers);

    /**
     * Returns true if required and candidate bean type match, false otherwise.
     *
     * @param requiredType Required bean type
     * @param beanType Candidate bean type
     * @return True if required type and bean type match, false otherwise
     */
    boolean matches(Type requiredType, Type beanType);

    /**
     * Returns true if provided bean matches required type, false otherwise
     *
     * @param bean Candidate bean
     * @param requiredType Required bean type
     * @return Returns true if given bean matches required type, false otherwise
     */
    boolean matchesType(BeanInfo bean, Type requiredType);

}
