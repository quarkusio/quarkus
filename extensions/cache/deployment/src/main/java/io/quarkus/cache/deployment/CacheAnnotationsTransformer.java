package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.AUGMENTED_CACHE_INVALIDATE;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.AUGMENTED_CACHE_INVALIDATE_ALL;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.AUGMENTED_CACHE_RESULT;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_ALL;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME_PARAMETER_NAME;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_RESULT;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.LOCK_TIMEOUT_PARAMETER_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.processor.AnnotationsTransformer;

/**
 * This class transforms at build time the caching API annotations (which have a mandatory {@code cacheName} parameter and can
 * only be used on methods) into augmented annotations that are used as interceptors bindings for the caching interceptors.
 */
public class CacheAnnotationsTransformer implements AnnotationsTransformer {

    @Override
    public boolean appliesTo(Kind kind) {
        return Kind.METHOD == kind;
    }

    @Override
    public void transform(TransformationContext context) {
        MethodInfo method = context.getTarget().asMethod();
        if (method.hasAnnotation(CACHE_RESULT)) {
            List<AnnotationValue> parameters = new ArrayList<>();
            parameters.add(getCacheName(method, CACHE_RESULT));
            findLockTimeout(method, CACHE_RESULT).ifPresent(parameters::add);
            context.transform().add(AUGMENTED_CACHE_RESULT, parameters.toArray(new AnnotationValue[parameters.size()])).done();
        }
        if (method.hasAnnotation(CACHE_INVALIDATE)) {
            AnnotationValue cacheName = getCacheName(method, CACHE_INVALIDATE);
            context.transform().add(AUGMENTED_CACHE_INVALIDATE, cacheName).done();
        }
        if (method.hasAnnotation(CACHE_INVALIDATE_ALL)) {
            AnnotationValue cacheName = getCacheName(method, CACHE_INVALIDATE_ALL);
            context.transform().add(AUGMENTED_CACHE_INVALIDATE_ALL, cacheName).done();
        }
    }

    private AnnotationValue getCacheName(MethodInfo method, DotName apiAnnotation) {
        String cacheName = method.annotation(apiAnnotation).value(CACHE_NAME_PARAMETER_NAME).asString();
        return AnnotationValue.createStringValue(CACHE_NAME_PARAMETER_NAME, cacheName);
    }

    private Optional<AnnotationValue> findLockTimeout(MethodInfo method, DotName apiAnnotation) {
        AnnotationValue lockTimeout = method.annotation(apiAnnotation).value(LOCK_TIMEOUT_PARAMETER_NAME);
        if (lockTimeout == null) {
            return Optional.empty();
        }
        return Optional.of(AnnotationValue.createLongalue(LOCK_TIMEOUT_PARAMETER_NAME, lockTimeout.asLong()));
    }
}
