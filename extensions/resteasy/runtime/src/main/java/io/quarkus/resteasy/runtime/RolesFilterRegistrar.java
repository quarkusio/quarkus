package io.quarkus.resteasy.runtime;

import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * A JAXRS provider that installs security filters to support the RBAC access to endpoints based on the
 * common security annotations.
 */
@Provider
public class RolesFilterRegistrar implements DynamicFeature {

    private static final DenyAllFilter denyAllFilter = new DenyAllFilter();
    private final Set<Class<? extends Annotation>> mpJwtAnnotations = new HashSet<>(
            asList(DenyAll.class, PermitAll.class, RolesAllowed.class));

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Annotation mpJwtAnnotation = getMpJwtAnnotation(resourceInfo);
        if (mpJwtAnnotation != null) {
            if (mpJwtAnnotation instanceof DenyAll) {
                configureDenyAll(context);
            } else if (mpJwtAnnotation instanceof RolesAllowed) {
                configureRolesAllowed((RolesAllowed) mpJwtAnnotation, context);
            }
        } else {
            // the resource method is not annotated and the class is not annotated either
            if (hasSecurityAnnotations(resourceInfo) && shouldNonannotatedMethodsBeDenied()) {
                // some other method has a security annotation and this one doesn't, it should be @DenyAll by default
                configureDenyAll(context);
            }
        }
    }

    private void configureRolesAllowed(RolesAllowed mpJwtAnnotation, FeatureContext context) {
        context.register(new RolesAllowedFilter(mpJwtAnnotation.value()));
    }

    private void configureDenyAll(FeatureContext context) {
        context.register(denyAllFilter);
    }

    private Annotation getMpJwtAnnotation(ResourceInfo resourceInfo) {
        Annotation annotation = getAnnotation(
                resourceInfo.getResourceMethod().getDeclaredAnnotations(),
                () -> resourceInfo.getResourceClass().getCanonicalName() + ":" + resourceInfo.getResourceMethod().getName());
        if (annotation == null) {
            annotation = getAnnotation(resourceInfo.getResourceMethod().getDeclaringClass().getDeclaredAnnotations(),
                    () -> resourceInfo.getResourceClass().getCanonicalName());
        }

        return annotation;
    }

    private Annotation getAnnotation(Annotation[] declaredAnnotations,
            Supplier<String> annotationPlacementDescriptor) {
        List<Annotation> annotations = Stream.of(declaredAnnotations)
                .filter(annotation -> mpJwtAnnotations.contains(annotation.annotationType()))
                .collect(Collectors.toList());
        switch (annotations.size()) {
            case 0:
                return null;
            case 1:
                return annotations.iterator().next();
            default:
                throw new RuntimeException("Duplicate MicroProfile JWT annotations found on "
                        + annotationPlacementDescriptor.get() +
                        ". Expected at most 1 annotation, found: " + annotations);
        }
    }

    private boolean hasSecurityAnnotations(ResourceInfo resource) {
        // resource methods are inherited (see JAX-RS spec, chapter 3.6)
        // resource methods must be `public` (see JAX-RS spec, chapter 3.3.1)
        // hence `resourceClass.getMethods` -- returns public methods, including inherited ones
        return Stream.of(resource.getResourceClass().getMethods())
                .filter(this::isResourceMethod)
                .anyMatch(this::hasSecurityAnnotations);
    }

    private boolean hasSecurityAnnotations(Method method) {
        return Stream.of(method.getAnnotations())
                .anyMatch(annotation -> mpJwtAnnotations.contains(annotation.annotationType()));
    }

    private boolean isResourceMethod(Method method) {
        // resource methods are methods annotated with an annotation that is itself annotated with @HttpMethod
        // (see JAX-RS spec, chapter 3.3)
        return Stream.of(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getAnnotation(HttpMethod.class) != null);
    }

    private boolean shouldNonannotatedMethodsBeDenied() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL resource = loader.getResource("/META-INF/MP-JWT-DENY-NONANNOTATED-METHODS");
        return resource != null;
    }
}
