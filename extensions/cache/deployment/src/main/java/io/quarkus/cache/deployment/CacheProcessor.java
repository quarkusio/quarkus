package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_ALL;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_ALL_LIST;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_INVALIDATE_LIST;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_KEY;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME_PARAM;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_RESULT;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.INTERCEPTORS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.INTERCEPTOR_BINDINGS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.INTERCEPTOR_BINDING_CONTAINERS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.MULTI;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.REGISTER_REST_CLIENT;
import static io.quarkus.cache.runtime.CacheBuildConfig.CAFFEINE_CACHE_TYPE;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.runtime.metrics.MetricsFactory.MICROMETER;
import static java.util.stream.Collectors.toList;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.deployment.exception.ClassTargetException;
import io.quarkus.cache.deployment.exception.KeyGeneratorConstructorException;
import io.quarkus.cache.deployment.exception.PrivateMethodTargetException;
import io.quarkus.cache.deployment.exception.UnsupportedRepeatedAnnotationException;
import io.quarkus.cache.deployment.exception.VoidReturnTypeTargetException;
import io.quarkus.cache.deployment.spi.AdditionalCacheNameBuildItem;
import io.quarkus.cache.deployment.spi.CacheManagerInfoBuildItem;
import io.quarkus.cache.deployment.spi.CacheTypeBuildItem;
import io.quarkus.cache.runtime.CacheBuildConfig;
import io.quarkus.cache.runtime.CacheInvalidateAllInterceptor;
import io.quarkus.cache.runtime.CacheInvalidateInterceptor;
import io.quarkus.cache.runtime.CacheManagerRecorder;
import io.quarkus.cache.runtime.CacheResultInterceptor;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.rest.client.reactive.spi.RestClientAnnotationsTransformerBuildItem;

class CacheProcessor {

    private static final Logger LOGGER = Logger.getLogger(CacheProcessor.class);

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
    RestClientAnnotationsTransformerBuildItem restClientAnnotationsTransformer() {
        return new RestClientAnnotationsTransformerBuildItem(new RestClientCacheAnnotationsTransformer());
    }

    @BuildStep
    CacheTypeBuildItem type(CacheBuildConfig config) {
        return new CacheTypeBuildItem(
                CAFFEINE_CACHE_TYPE.equals(config.type()) ? CacheTypeBuildItem.Type.LOCAL : CacheTypeBuildItem.Type.REMOTE);
    }

    @BuildStep
    void validateCacheAnnotationsAndProduceCacheNames(CombinedIndexBuildItem combinedIndex,
            List<AdditionalCacheNameBuildItem> additionalCacheNames,
            List<io.quarkus.cache.deployment.AdditionalCacheNameBuildItem> additionalCacheNamesDeprecated,
            BuildProducer<ValidationErrorBuildItem> validationErrors,
            BuildProducer<CacheNamesBuildItem> cacheNames, BeanDiscoveryFinishedBuildItem beanDiscoveryFinished) {

        // Validation errors produced by this build step.
        List<Throwable> throwables = new ArrayList<>();
        // Cache names produced by this build step.
        Set<String> names = new HashSet<>();
        // The cache key generators constructors are validated at the end of this build step.
        Set<DotName> keyGenerators = new HashSet<>();

        /*
         * First, for each non-repeated cache interceptor binding:
         * - its target is validated
         * - the corresponding cache name is collected
         */
        for (DotName bindingName : INTERCEPTOR_BINDINGS) {
            for (AnnotationInstance binding : combinedIndex.getIndex().getAnnotations(bindingName)) {
                throwables.addAll(validateInterceptorBindingTarget(binding, binding.target()));
                findCacheKeyGenerator(binding, binding.target()).ifPresent(keyGenerators::add);
                if (binding.target().kind() == METHOD) {
                    /*
                     * Cache names from the interceptor bindings placed on cache interceptors must not be collected to prevent
                     * the instantiation of a cache with an empty name.
                     */
                    names.add(binding.value(CACHE_NAME_PARAM).asString());
                }
            }
        }

        // The exact same things need to be done for repeated cache interceptor bindings.
        for (DotName containerName : INTERCEPTOR_BINDING_CONTAINERS) {
            for (AnnotationInstance container : combinedIndex.getIndex().getAnnotations(containerName)) {
                for (AnnotationInstance binding : container.value("value").asNestedArray()) {
                    throwables.addAll(validateInterceptorBindingTarget(binding, container.target()));
                    findCacheKeyGenerator(binding, container.target()).ifPresent(keyGenerators::add);
                    names.add(binding.value(CACHE_NAME_PARAM).asString());
                }
                /*
                 * Interception from repeated interceptor bindings won't work with the CDI implementation from MicroProfile REST
                 * Client. Using repeated interceptor bindings on a method from a class annotated with @RegisterRestClient must
                 * therefore be forbidden.
                 */
                if (container.target().kind() == METHOD) {
                    MethodInfo methodInfo = container.target().asMethod();
                    if (methodInfo.declaringClass().declaredAnnotation(REGISTER_REST_CLIENT) != null) {
                        throwables.add(new UnsupportedRepeatedAnnotationException(methodInfo));
                    }
                }
            }
        }

        // Let's also collect the cache names from the @CacheName annotations.
        for (AnnotationInstance qualifier : combinedIndex.getIndex().getAnnotations(CACHE_NAME)) {
            // The @CacheName annotation from CacheProducer must be ignored.
            if (qualifier.target().kind() == METHOD) {
                /*
                 * This should only happen in CacheProducer. It'd be nice if we could forbid using @CacheName on a method in
                 * any other class, but Arc throws an AmbiguousResolutionException before we get a chance to validate things
                 * here.
                 */
            } else {
                names.add(qualifier.value().asString());
            }
        }

        // Finally, additional cache names provided by other extensions must be added to the cache names collection.
        for (AdditionalCacheNameBuildItem additionalCacheName : additionalCacheNames) {
            names.add(additionalCacheName.getName());
        }
        for (io.quarkus.cache.deployment.AdditionalCacheNameBuildItem additionalCacheName : additionalCacheNamesDeprecated) {
            names.add(additionalCacheName.getName());
        }
        cacheNames.produce(new CacheNamesBuildItem(names));

        if (!keyGenerators.isEmpty()) {
            throwables.addAll(validateKeyGenerators(combinedIndex, beanDiscoveryFinished, keyGenerators));
        }

        validationErrors.produce(new ValidationErrorBuildItem(throwables.toArray(new Throwable[0])));
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
                if (CACHE_RESULT.equals(binding.name())) {
                    if (methodInfo.returnType().kind() == Type.Kind.VOID) {
                        throwables.add(new VoidReturnTypeTargetException(methodInfo));
                    } else if (MULTI.equals(methodInfo.returnType().name())) {
                        LOGGER.warnf("@CacheResult is not currently supported on a method returning %s [class=%s, method=%s]",
                                MULTI, methodInfo.declaringClass().name(), methodInfo.name());
                    }
                }
                break;
            default:
                // This should never be thrown.
                throw new DeploymentException("Unexpected cache interceptor binding target: " + target.kind());
        }
        return throwables;
    }

    private Optional<DotName> findCacheKeyGenerator(AnnotationInstance binding, AnnotationTarget target) {
        if (target.kind() == METHOD && (CACHE_RESULT.equals(binding.name()) || CACHE_INVALIDATE.equals(binding.name()))) {
            AnnotationValue keyGenerator = binding.value("keyGenerator");
            if (keyGenerator != null) {
                return Optional.of(keyGenerator.asClass().name());
            }
        }
        return Optional.empty();
    }

    private List<Throwable> validateKeyGenerators(CombinedIndexBuildItem combinedIndex,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished, Set<DotName> keyGenerators) {
        List<Throwable> throwables = new ArrayList<>();
        for (DotName keyGenClassName : keyGenerators) {
            List<BeanInfo> beans = beanDiscoveryFinished.beanStream().withBeanType(keyGenClassName).collect();
            if (beans.isEmpty()) {
                // Key generators must have a default constructor if they are not CDI beans
                ClassInfo keyGenClassInfo = combinedIndex.getIndex().getClassByName(keyGenClassName);
                if (keyGenClassInfo == null) {
                    throwables.add(new IllegalStateException(
                            "Unable to find the key generator class in the index:" + keyGenClassName));
                } else if (!keyGenClassInfo.hasNoArgsConstructor()) {
                    throwables.add(new KeyGeneratorConstructorException(keyGenClassInfo));
                }
            } else if (beans.size() > 1) {
                String message = String.format(
                        "There must be exactly one bean that matches the key generator class: \"%s\"\n\t- beans: %s",
                        keyGenClassName, beans);
                throwables.add(new IllegalStateException(message));
            }
        }
        return throwables;
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void cacheManagerInfos(BuildProducer<CacheManagerInfoBuildItem> producer,
            Optional<MetricsCapabilityBuildItem> metricsCapability, CacheManagerRecorder recorder) {
        producer.produce(new CacheManagerInfoBuildItem(recorder.noOpCacheManagerInfo()));
        producer.produce(new CacheManagerInfoBuildItem(recorder.getCacheManagerInfoWithoutMetrics()));
        if (metricsCapability.isPresent() && metricsCapability.get().metricsSupported(MICROMETER)) {
            // if we include this unconditionally the native image building will fail when Micrometer is not around
            producer.produce(new CacheManagerInfoBuildItem(recorder.getCacheManagerInfoWithMicrometerMetrics()));
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    SyntheticBeanBuildItem configureCacheManagerSyntheticBean(List<CacheManagerInfoBuildItem> infos,
            CacheNamesBuildItem cacheNames, Optional<MetricsCapabilityBuildItem> metricsCapability,
            CacheManagerRecorder cacheManagerRecorder) {

        boolean micrometerSupported = metricsCapability.isPresent() && metricsCapability.get().metricsSupported(MICROMETER);
        Supplier<CacheManager> cacheManagerSupplier = cacheManagerRecorder.resolveCacheInfo(
                infos.stream().map(CacheManagerInfoBuildItem::get).collect(toList()), cacheNames.getNames(),
                micrometerSupported);

        return SyntheticBeanBuildItem.configure(CacheManager.class)
                .scope(ApplicationScoped.class)
                .supplier(cacheManagerSupplier)
                .setRuntimeInit()
                .done();
    }

    @BuildStep
    List<BytecodeTransformerBuildItem> enhanceRestClientMethods(CombinedIndexBuildItem combinedIndex,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        List<BytecodeTransformerBuildItem> bytecodeTransformers = new ArrayList<>();
        boolean cacheInvalidate = false;
        boolean cacheResult = false;
        boolean cacheInvalidateAll = false;

        for (AnnotationInstance registerRestClientAnnotation : combinedIndex.getIndex().getAnnotations(REGISTER_REST_CLIENT)) {
            if (registerRestClientAnnotation.target().kind() == Kind.CLASS) {
                ClassInfo classInfo = registerRestClientAnnotation.target().asClass();
                for (MethodInfo methodInfo : classInfo.methods()) {
                    boolean transform = false;

                    if (methodInfo.hasAnnotation(CACHE_INVALIDATE) || methodInfo.hasAnnotation(CACHE_INVALIDATE_LIST)) {
                        transform = true;
                        cacheInvalidate = true;
                    }
                    if (methodInfo.hasAnnotation(CACHE_RESULT)) {
                        transform = true;
                        cacheResult = true;
                    }
                    if (methodInfo.hasAnnotation(CACHE_INVALIDATE_ALL) || methodInfo.hasAnnotation(CACHE_INVALIDATE_ALL_LIST)) {
                        cacheInvalidateAll = true;
                    }

                    if (transform) {
                        short[] cacheKeyParameterPositions = getCacheKeyParameterPositions(methodInfo);
                        /*
                         * The bytecode transformation is always performed even if `cacheKeyParameterPositions` is empty because
                         * the method parameters would be inspected using reflection at run time otherwise.
                         */
                        bytecodeTransformers.add(new BytecodeTransformerBuildItem(classInfo.toString(),
                                new RestClientMethodEnhancer(methodInfo.name(), cacheKeyParameterPositions)));
                    }
                }
            }
        }

        // Interceptors need to be registered as unremovable due to the rest-client integration - interceptors
        // are currently resolved dynamically at runtime because per the spec interceptor bindings cannot be declared on interfaces
        if (cacheResult) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(CacheResultInterceptor.class.getName()));
        }
        if (cacheInvalidate) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(CacheInvalidateInterceptor.class.getName()));
        }
        if (cacheInvalidateAll) {
            unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(CacheInvalidateAllInterceptor.class.getName()));
        }
        return bytecodeTransformers;
    }

    /**
     * Returns an array containing the positions of the given method parameters annotated with
     * {@link io.quarkus.cache.CacheKey @CacheKey}, or an empty array if no such parameter is found.
     *
     * @param methodInfo method info
     * @return cache key parameters positions
     */
    private short[] getCacheKeyParameterPositions(MethodInfo methodInfo) {
        List<Short> positions = new ArrayList<>();
        for (AnnotationInstance annotation : methodInfo.annotations(CACHE_KEY)) {
            positions.add(annotation.target().asMethodParameter().position());
        }
        short[] result = new short[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            result[i] = positions.get(i);
        }
        return result;
    }
}
