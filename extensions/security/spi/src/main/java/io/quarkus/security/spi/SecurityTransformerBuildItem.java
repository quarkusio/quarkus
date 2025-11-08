package io.quarkus.security.spi;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationOverlay;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Declaration;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.security.spi.SecurityTransformer.AuthorizationType;

/**
 * A build item that serves as a builder for the {@link SecurityTransformer}.
 */
public final class SecurityTransformerBuildItem extends SimpleBuildItem {

    private record SecurityTransformerCache(IndexView indexView, SecurityTransformer transformer) {
    }

    private final Map<AuthorizationType, Set<DotName>> authorizationTypeToSecurityAnnotations;
    private final Set<DotName> allSecurityAnnotations;
    private final Map<IndexView, SecurityTransformerCache> securityTransformerCache;

    public SecurityTransformerBuildItem(Map<AuthorizationType, Set<DotName>> authorizationTypeToSecurityAnnotations) {
        this.authorizationTypeToSecurityAnnotations = Collections.unmodifiableMap(authorizationTypeToSecurityAnnotations);
        this.allSecurityAnnotations = getAllSecurityAnnotations(authorizationTypeToSecurityAnnotations);
        this.securityTransformerCache = new ConcurrentHashMap<>();
    }

    public static SecurityTransformer createSecurityTransformer(IndexView indexView,
            Optional<SecurityTransformerBuildItem> optionalTransformerBuildItem) {
        return createSecurityTransformer(indexView, optionalTransformerBuildItem.orElseThrow());
    }

    public static SecurityTransformer createSecurityTransformer(IndexView indexView,
            SecurityTransformerBuildItem transformerBuildItem) {
        return transformerBuildItem.getOrCreateTransformer(indexView);
    }

    public String[] getAllSecurityAnnotationNames() {
        return allSecurityAnnotations.stream().map(DotName::toString).toArray(String[]::new);
    }

    private SecurityTransformer getOrCreateTransformer(IndexView indexView) {
        // this is cached because the annotation overlay has some cache which we can leverage
        return securityTransformerCache.computeIfAbsent(indexView, index -> {
            // create transformer
            var transformer = new SecurityTransformerImpl(
                    AnnotationOverlay.builder(index, null).build(),
                    null, List.of());
            return new SecurityTransformerCache(index, transformer);
        }).transformer;
    }

    private Set<DotName> getSecurityAnnotations(AuthorizationType[] authorizationTypes) {
        if (authorizationTypes == null || authorizationTypes.length == 0
                || authorizationTypes.length == AuthorizationType.values().length) {
            return allSecurityAnnotations;
        }
        Set<DotName> result = new HashSet<>();
        for (var authorizationType : authorizationTypes) {
            var securityAnnotations = authorizationTypeToSecurityAnnotations.get(authorizationType);
            if (securityAnnotations != null) {
                result.addAll(securityAnnotations);
            }
        }
        return result;
    }

    private boolean hasSecurityAnnotationDetectedByIndex(Declaration declaration, IndexView index) {
        if (declaration.declaredAnnotations().stream().anyMatch(this::isSecurityAnnotation)) {
            return true;
        }
        return declaration.declaredAnnotations().stream().anyMatch(ai -> isSecurityMetaAnnotation(ai, index));
    }

    private boolean hasSecurityAnnotationDetectedByIndex(Declaration declaration) {
        // this method does not consider meta-annotations
        return declaration.declaredAnnotations().stream().anyMatch(this::isSecurityAnnotation);
    }

    private Collection<AnnotationInstance> getSecurityAnnotations(MethodInfo methodInfo, IndexView index) {
        var securityAnnotations = new ArrayList<>(allSecurityAnnotations.stream()
                .map(sa -> methodInfo.declaredAnnotationsWithRepeatable(sa, index))
                .flatMap(Collection::stream)
                .toList());
        // meta-annotations like @CanWrite with @PermissionsAllowed("write")
        securityAnnotations.addAll(methodInfo.declaredAnnotations().stream()
                .filter(ai -> isSecurityMetaAnnotation(ai, index))
                .map(AnnotationInstance::name)
                .map(sa -> methodInfo.declaredAnnotationsWithRepeatable(sa, index))
                .flatMap(Collection::stream)
                .toList());
        if (securityAnnotations.isEmpty()) {
            // there are no method-level security annotations, so try inherited class-level security annotations instead
            securityAnnotations.addAll(allSecurityAnnotations.stream()
                    .map(sa -> methodInfo.declaringClass().declaredAnnotationsWithRepeatable(sa, index))
                    .flatMap(Collection::stream)
                    .toList());
            securityAnnotations.addAll(methodInfo.declaringClass().declaredAnnotations().stream()
                    .filter(ai -> isSecurityMetaAnnotation(ai, index))
                    .map(AnnotationInstance::name)
                    .map(sa -> methodInfo.declaringClass().declaredAnnotationsWithRepeatable(sa, index))
                    .flatMap(Collection::stream)
                    .toList());
        }
        return Collections.unmodifiableCollection(securityAnnotations);
    }

    private boolean isSecurityAnnotation(AnnotationInstance ai) {
        return allSecurityAnnotations.contains(ai.name());
    }

    private boolean isSecurityMetaAnnotation(AnnotationInstance ai, IndexView index) {
        var annotationClass = index.getClassByName(ai.name());
        if (annotationClass != null) {
            // this does not consider repeatable annotations because we don't support them yet
            for (AnnotationInstance declaredAnnotation : annotationClass.declaredAnnotations()) {
                if (isSecurityAnnotation(declaredAnnotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<DotName> getAllSecurityAnnotations(
            Map<AuthorizationType, Set<DotName>> authorizationTypeToSecurityAnnotations) {
        return authorizationTypeToSecurityAnnotations.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    final class SecurityTransformerImpl implements SecurityTransformer {

        private final AnnotationOverlay annotationOverlay;
        private final Collection<AnnotationTransformation> interfaceTransformations;
        private final Collection<DotName> possiblySecuredParentInterfaces;

        private SecurityTransformerImpl(AnnotationOverlay annotationOverlay,
                Collection<AnnotationTransformation> interfaceTransformations,
                Collection<DotName> possiblySecuredParentInterfaces) {
            this.annotationOverlay = annotationOverlay;
            this.interfaceTransformations = interfaceTransformations;
            this.possiblySecuredParentInterfaces = possiblySecuredParentInterfaces;
        }

        @Override
        public Collection<AnnotationInstance> getAnnotations(DotName securityAnnotationName) {
            return getAnnotations(securityAnnotationName, false);
        }

        @Override
        public Collection<AnnotationInstance> getAnnotationsWithRepeatable(DotName securityAnnotationName) {
            return getAnnotations(securityAnnotationName, true);
        }

        @Override
        public boolean hasSecurityAnnotation(AnnotationTarget annotationTarget, AuthorizationType... authorizationTypes) {
            return findFirstSecurityAnnotation(annotationTarget, authorizationTypes).isPresent();
        }

        @Override
        public boolean isSecurityAnnotation(AnnotationInstance annotationInstance, AuthorizationType... authorizationTypes) {
            if (authorizationTypes == null || authorizationTypes.length == 0) {
                authorizationTypes = AuthorizationType.values();
            }
            var securityAnnotationName = annotationInstance.name();
            for (var authorizationType : authorizationTypes) {
                var securityAnnotations = authorizationTypeToSecurityAnnotations.get(authorizationType);
                if (securityAnnotations != null && securityAnnotations.contains(securityAnnotationName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isSecurityAnnotation(Collection<AnnotationInstance> annotationInstances) {
            for (AnnotationInstance annotationInstance : annotationInstances) {
                if (isSecurityAnnotation(annotationInstance)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Optional<AnnotationInstance> findFirstSecurityAnnotation(Collection<AnnotationInstance> annotationInstances) {
            for (AnnotationInstance annotationInstance : annotationInstances) {
                if (isSecurityAnnotation(annotationInstance)) {
                    return Optional.of(annotationInstance);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<AnnotationInstance> findFirstSecurityAnnotation(AnnotationTarget annotationTarget,
                AuthorizationType... authorizationTypes) {
            return findFirstSecurityAnnotation(annotationTarget.asDeclaration(), getSecurityAnnotations(authorizationTypes));
        }

        @Override
        public Collection<AnnotationTransformation> getInterfaceTransformations() {
            return interfaceTransformations;
        }

        @Override
        public Collection<DotName> getSecurityAnnotationNames(AuthorizationType... authorizationTypes) {
            return getSecurityAnnotations(authorizationTypes);
        }

        private Optional<AnnotationInstance> findFirstSecurityAnnotation(Declaration declaration,
                Set<DotName> securityAnnotations) {
            for (AnnotationInstance instance : annotationOverlay.annotations(declaration)) {
                if (securityAnnotations.contains(instance.name())) {
                    return Optional.of(instance);
                }
            }
            return Optional.empty();
        }

        private boolean shouldCheckForSecurityAnnotations(ClassInfo ci, HashSet<String> checkedInterfaces) {
            return possiblySecuredParentInterfaces.contains(ci.name()) && checkedInterfaces.add(ci.name().toString());
        }

        private Collection<AnnotationInstance> getImplementorsSecurityAnnotations(DotName securityAnnotationName,
                ClassInfo securedInterface, boolean repeatable) {
            Collection<AnnotationInstance> result = null;
            // secured interface implementations may have their security annotations added by the annotation
            // transformer which is not visible for the index, therefore we go over all the implementors and
            // collect for each secured method we must add its secured implementor method if it wasn't found
            // directly by the index, we leverage annotation transformations and find not indexed secured methods

            // this will go over all the implementation methods and catch all security annotations added by the transformer;
            // naturally it may happen that some method will be collected more than once, which is why we must store
            // method infos in a set
            for (var implementation : annotationOverlay.index().getAllKnownImplementations(securedInterface.name())) {
                // we don't need to care about the class-level transformation annotations, because we only support method
                // security and all the interface class-level annotations are transformed to individual methods instead
                for (var implementationMethod : implementation.methods()) {
                    // for now, this will not consider meta-annotations on purpose, because ATM we handle them separately
                    if (hasSecurityAnnotationDetectedByIndex(implementationMethod)) {
                        // this annotation was indexed, therefore already collected
                        continue;
                    }
                    if (repeatable) {
                        var annotations = annotationOverlay
                                .annotationsWithRepeatable(implementationMethod, securityAnnotationName)
                                .stream()
                                .map(interfaceInstance -> AnnotationInstance.builder(interfaceInstance.name())
                                        .addAll(interfaceInstance.values())
                                        .buildWithTarget(implementationMethod))
                                .toList();
                        if (!annotations.isEmpty()) {
                            if (result == null) {
                                result = new HashSet<>();
                            }
                            result.addAll(annotations);
                        }
                    } else {
                        if (annotationOverlay.hasAnnotation(implementationMethod, securityAnnotationName)) {
                            if (result == null) {
                                result = new HashSet<>();
                            }
                            var interfaceInstance = annotationOverlay.annotation(implementationMethod, securityAnnotationName);
                            var implementationInstance = AnnotationInstance.builder(interfaceInstance.name())
                                    .addAll(interfaceInstance.values())
                                    .buildWithTarget(implementationMethod);
                            result.add(implementationInstance);
                        }
                    }
                }
            }
            return result;
        }

        private Collection<AnnotationInstance> getAnnotations(DotName securityAnnotationName, boolean repeatable) {
            final Collection<AnnotationInstance> indexedAnnotationInstances;
            if (repeatable) {
                indexedAnnotationInstances = annotationOverlay.index().getAnnotationsWithRepeatable(securityAnnotationName,
                        annotationOverlay.index());
            } else {
                indexedAnnotationInstances = annotationOverlay.index().getAnnotations(securityAnnotationName);
            }

            if (interfaceTransformations == null || indexedAnnotationInstances.isEmpty()) {
                return indexedAnnotationInstances;
            }

            var checkedInterfaces = new HashSet<String>();
            // add security annotation instances from interfaces direct implementors
            var result = new HashSet<>(indexedAnnotationInstances);
            for (var annotationInstance : indexedAnnotationInstances) {
                final ClassInfo declaringClass;
                if (annotationInstance.target().kind() == AnnotationTarget.Kind.METHOD) {
                    declaringClass = annotationInstance.target().asMethod().declaringClass();
                } else if (annotationInstance.target().kind() == AnnotationTarget.Kind.CLASS) {
                    declaringClass = annotationInstance.target().asClass();
                } else {
                    // illegal state - this shouldn't happen
                    continue;
                }
                if (shouldCheckForSecurityAnnotations(declaringClass, checkedInterfaces)) {
                    // test that secured interface doesn't have default methods with security annotations
                    // as CDI interceptors are not applied on them
                    for (var securedInterfaceMethod : declaringClass.methods()) {
                        if (securedInterfaceMethod.isDefault()
                                && hasSecurityAnnotationDetectedByIndex(securedInterfaceMethod, annotationOverlay.index())) {
                            throw new RuntimeException("""
                                    Interface '%s' default method '%s' has security annotation.
                                    Securing interface default methods is currently not supported, please secure
                                    the interface implementation method instead.
                                    """.formatted(declaringClass.name().toString(), securedInterfaceMethod.name()));
                        }
                    }

                    var implementorSecurityAnnotation = getImplementorsSecurityAnnotations(securityAnnotationName,
                            declaringClass, repeatable);
                    if (implementorSecurityAnnotation != null) {
                        result.addAll(implementorSecurityAnnotation);
                    }
                }
            }
            return Collections.unmodifiableCollection(result);
        }
    }

}
