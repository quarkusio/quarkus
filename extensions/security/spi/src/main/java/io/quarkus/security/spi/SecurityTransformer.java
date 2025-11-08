package io.quarkus.security.spi;

import java.util.Collection;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

import io.quarkus.security.spi.SecurityTransformerBuildItem.SecurityTransformerImpl;

/**
 * Helper class that allows to determine whether the annotation target has security annotations and which ones.
 */
public sealed interface SecurityTransformer permits SecurityTransformerImpl {

    /**
     * Types of authorization we perform for registered security annotations.
     * If no authorization type is specified, this helper will apply all the authorization type values.
     */
    enum AuthorizationType {
        /**
         * Security checks are performed for CDI beans and endpoints annotated with security annotations.
         */
        SECURITY_CHECK,
        /**
         * Authorization policies are performed for incoming requests.
         * They can be either global or restricted to certain methods by annotations.
         */
        AUTHORIZATION_POLICY
    }

    Collection<AnnotationInstance> getAnnotations(DotName securityAnnotationName);

    Collection<AnnotationInstance> getAnnotationsWithRepeatable(DotName securityAnnotationName);

    boolean hasSecurityAnnotation(AnnotationTarget annotationTarget, AuthorizationType... authorizationTypes);

    boolean isSecurityAnnotation(AnnotationInstance annotationInstance, AuthorizationType... authorizationTypes);

    boolean isSecurityAnnotation(Collection<AnnotationInstance> annotationInstances);

    Optional<AnnotationInstance> findFirstSecurityAnnotation(Collection<AnnotationInstance> annotationInstances);

    Optional<AnnotationInstance> findFirstSecurityAnnotation(AnnotationTarget annotationTarget,
            AuthorizationType... authorizationTypes);

    Collection<AnnotationTransformation> getInterfaceTransformations();

    Collection<DotName> getSecurityAnnotationNames(AuthorizationType... authorizationTypes);
}
