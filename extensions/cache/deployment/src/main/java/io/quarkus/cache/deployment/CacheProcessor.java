package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME_PARAM;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_RESULT;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.INTERCEPTORS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.INTERCEPTOR_BINDINGS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.INTERCEPTOR_BINDING_CONTAINERS;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.cache.deployment.exception.ClassTargetException;
import io.quarkus.cache.deployment.exception.PrivateMethodTargetException;
import io.quarkus.cache.deployment.exception.UnknownCacheNameException;
import io.quarkus.cache.deployment.exception.UnsupportedRepeatedAnnotationException;
import io.quarkus.cache.deployment.exception.VoidReturnTypeTargetException;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheBuildRecorder;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheInfo;
import io.quarkus.cache.runtime.noop.NoOpCacheBuildRecorder;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class CacheProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.CACHE);
    }

    @BuildStep
    AutoInjectAnnotationBuildItem autoInjectCacheName() {
        return new AutoInjectAnnotationBuildItem(CACHE_NAME);
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationsTransformer() {
        return new AnnotationsTransformerBuildItem(new CacheAnnotationsTransformer());
    }

    @BuildStep
    void validateCacheAnnotationsAndProduceCacheNames(CombinedIndexBuildItem combinedIndex,
            List<AdditionalCacheNameBuildItem> additionalCacheNames, BuildProducer<ValidationErrorBuildItem> validationErrors,
            BuildProducer<CacheNamesBuildItem> cacheNames) {

        // Validation errors produced by this build step.
        List<Throwable> throwables = new ArrayList<>();
        // Cache names produced by this build step.
        Set<String> names = new HashSet<>();

        /*
         * First, for each non-repeated cache interceptor binding:
         * - its target is validated
         * - the corresponding cache name is collected
         */
        for (DotName bindingName : INTERCEPTOR_BINDINGS) {
            for (AnnotationInstance binding : combinedIndex.getIndex().getAnnotations(bindingName)) {
                throwables.addAll(validateInterceptorBindingTarget(binding, binding.target()));
                names.add(binding.value(CACHE_NAME_PARAM).asString());
            }
        }

        // The exact same things need to be done for repeated cache interceptor bindings.
        for (DotName containerName : INTERCEPTOR_BINDING_CONTAINERS) {
            for (AnnotationInstance container : combinedIndex.getIndex().getAnnotations(containerName)) {
                for (AnnotationInstance binding : container.value("value").asNestedArray()) {
                    throwables.addAll(validateInterceptorBindingTarget(binding, container.target()));
                    names.add(binding.value(CACHE_NAME_PARAM).asString());
                }
                /*
                 * Interception from repeated interceptor bindings won't work with the CDI implementation from MicroProfile REST
                 * Client. Using repeated interceptor bindings on a method from a class annotated with @RegisterRestClient must
                 * therefore be forbidden.
                 */
                if (container.target().kind() == Kind.METHOD) {
                    MethodInfo methodInfo = container.target().asMethod();
                    if (methodInfo.declaringClass().classAnnotation(CacheDeploymentConstants.REGISTER_REST_CLIENT) != null) {
                        throwables.add(new UnsupportedRepeatedAnnotationException(methodInfo));
                    }
                }
            }
        }

        /*
         * Before @CacheName can be validated, additional cache names provided by other extensions must be added to the cache
         * names collection built above.
         */
        for (AdditionalCacheNameBuildItem additionalCacheName : additionalCacheNames) {
            names.add(additionalCacheName.getName());
        }

        // @CacheName can now be validated.
        for (AnnotationInstance qualifier : combinedIndex.getIndex().getAnnotations(CACHE_NAME)) {
            String cacheName = qualifier.value().asString();
            AnnotationTarget target = qualifier.target();
            switch (target.kind()) {
                case FIELD:
                    if (!names.contains(cacheName)) {
                        ClassInfo declaringClass = target.asField().declaringClass();
                        throwables.add(new UnknownCacheNameException(declaringClass.name(), cacheName));
                    }
                    break;
                case METHOD:
                    /*
                     * This should only happen in CacheProducer. It'd be nice if we could forbid using @CacheName in any other
                     * class, but Arc throws an AmbiguousResolutionException before we get a chance to validate things here.
                     */
                    break;
                case METHOD_PARAMETER:
                    if (!names.contains(cacheName)) {
                        ClassInfo declaringClass = target.asMethodParameter().method().declaringClass();
                        throwables.add(new UnknownCacheNameException(declaringClass.name(), cacheName));
                    }
                    break;
                default:
                    // This should never be thrown.
                    throw new DeploymentException("Unexpected @CacheName target: " + target.kind());
            }
        }

        validationErrors.produce(new ValidationErrorBuildItem(throwables.toArray(new Throwable[0])));
        cacheNames.produce(new CacheNamesBuildItem(names));
    }

    private List<Throwable> validateInterceptorBindingTarget(AnnotationInstance binding, AnnotationTarget target) {
        List<Throwable> throwables = new ArrayList<>();
        switch (target.kind()) {
            case CLASS:
                ClassInfo classInfo = target.asClass();
                if (!INTERCEPTORS.contains(classInfo.name())) {
                    throwables.add(new ClassTargetException(classInfo.name(), binding.name()));
                }
                break;
            case METHOD:
                MethodInfo methodInfo = target.asMethod();
                if (Modifier.isPrivate(methodInfo.flags())) {
                    throwables.add(new PrivateMethodTargetException(methodInfo, binding.name()));
                }
                if (CACHE_RESULT.equals(binding.name()) && methodInfo.returnType().kind() == Type.Kind.VOID) {
                    throwables.add(new VoidReturnTypeTargetException(methodInfo));
                }
                break;
            default:
                // This should never be thrown.
                throw new DeploymentException("Unexpected cache interceptor binding target: " + target.kind());
        }
        return throwables;
    }

    @BuildStep
    @Record(STATIC_INIT)
    void recordCachesBuild(CacheNamesBuildItem cacheNames, CacheConfig config, BeanContainerBuildItem beanContainer,
            CaffeineCacheBuildRecorder caffeineRecorder, NoOpCacheBuildRecorder noOpRecorder) {
        if (cacheNames.getNames().size() > 0) {
            if (config.enabled) {
                switch (config.type) {
                    case CacheDeploymentConstants.CAFFEINE_CACHE_TYPE:
                        Set<CaffeineCacheInfo> cacheInfos = CaffeineCacheInfoBuilder.build(cacheNames.getNames(), config);
                        caffeineRecorder.buildCaches(beanContainer.getValue(), cacheInfos);
                        break;
                    default:
                        throw new DeploymentException("Unknown cache type: " + config.type);
                }
            } else {
                noOpRecorder.buildCaches(beanContainer.getValue(), cacheNames.getNames());
            }
        }
    }
}
