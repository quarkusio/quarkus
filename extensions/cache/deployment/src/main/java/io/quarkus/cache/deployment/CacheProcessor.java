package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.API_METHODS_ANNOTATIONS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.API_METHODS_ANNOTATIONS_LISTS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME_PARAM;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_PRODUCER;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.cache.runtime.CacheInvalidateAllInterceptor;
import io.quarkus.cache.runtime.CacheInvalidateInterceptor;
import io.quarkus.cache.runtime.CacheResultInterceptor;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheBuildRecorder;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheInfo;
import io.quarkus.cache.runtime.noop.NoOpCacheBuildRecorder;
import io.quarkus.deployment.Feature;
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
    List<AdditionalBeanBuildItem> additionalBeans() {
        return Arrays.asList(
                new AdditionalBeanBuildItem(CacheInvalidateAllInterceptor.class),
                new AdditionalBeanBuildItem(CacheInvalidateInterceptor.class),
                new AdditionalBeanBuildItem(CacheResultInterceptor.class));
    }

    @BuildStep
    ValidationErrorBuildItem validateBeanDeployment(ValidationPhaseBuildItem validationPhase) {
        AnnotationStore annotationStore = validationPhase.getContext().get(Key.ANNOTATION_STORE);
        List<Throwable> throwables = new ArrayList<>();
        for (BeanInfo bean : validationPhase.getContext().get(Key.BEANS)) {
            if (bean.isClassBean()) {
                for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                    if (annotationStore.hasAnyAnnotation(method, API_METHODS_ANNOTATIONS)) {
                        CacheMethodValidator.validateAnnotations(annotationStore, bean, method, throwables);
                    }
                }
            }
        }
        return new ValidationErrorBuildItem(throwables.toArray(new Throwable[0]));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void recordCachesBuild(CombinedIndexBuildItem combinedIndex, BeanContainerBuildItem beanContainer, CacheConfig config,
            CaffeineCacheBuildRecorder caffeineRecorder, NoOpCacheBuildRecorder noOpRecorder,
            List<AdditionalCacheNameBuildItem> additionalCacheNames) {
        Set<String> cacheNames = getCacheNames(combinedIndex.getIndex(), additionalCacheNames);
        validateCacheNameAnnotations(combinedIndex.getIndex(), cacheNames);
        if (cacheNames.size() > 0) {
            if (config.enabled) {
                switch (config.type) {
                    case CacheDeploymentConstants.CAFFEINE_CACHE_TYPE:
                        Set<CaffeineCacheInfo> cacheInfos = CaffeineCacheInfoBuilder.build(cacheNames, config);
                        caffeineRecorder.buildCaches(beanContainer.getValue(), cacheInfos);
                        break;
                    default:
                        throw new DeploymentException("Unknown cache type: " + config.type);
                }
            } else {
                noOpRecorder.buildCaches(beanContainer.getValue(), cacheNames);
            }
        }
    }

    private Set<String> getCacheNames(IndexView index, List<AdditionalCacheNameBuildItem> additionalCacheNames) {
        Set<String> cacheNames = new HashSet<>();
        for (DotName cacheAnnotation : API_METHODS_ANNOTATIONS) {
            for (AnnotationInstance annotation : index.getAnnotations(cacheAnnotation)) {
                if (annotation.target().kind() == METHOD) {
                    cacheNames.add(annotation.value(CACHE_NAME_PARAM).asString());
                }
            }
        }
        for (DotName list : API_METHODS_ANNOTATIONS_LISTS) {
            for (AnnotationInstance annotation : index.getAnnotations(list)) {
                if (annotation.target().kind() == METHOD) {
                    for (AnnotationInstance nestedAnnotation : annotation.value("value").asNestedArray()) {
                        cacheNames.add(nestedAnnotation.value(CACHE_NAME_PARAM).asString());
                    }
                }
            }
        }
        for (AdditionalCacheNameBuildItem additionalCacheName : additionalCacheNames) {
            cacheNames.add(additionalCacheName.getName());
        }
        return cacheNames;
    }

    private void validateCacheNameAnnotations(IndexView index, Set<String> cacheNames) {
        for (AnnotationInstance cacheNameAnnotation : index.getAnnotations(CACHE_NAME)) {
            AnnotationTarget target = cacheNameAnnotation.target();
            if (target.kind() == Kind.FIELD || target.kind() == Kind.METHOD_PARAMETER) {
                String cacheName = cacheNameAnnotation.value().asString();
                if (!cacheNames.contains(cacheName)) {
                    ClassInfo declaringClass;
                    if (target.kind() == Kind.FIELD) {
                        declaringClass = target.asField().declaringClass();
                    } else {
                        declaringClass = target.asMethodParameter().method().declaringClass();
                    }
                    throw new DeploymentException(
                            "A field or method parameter is annotated with @CacheName(\"" + cacheName + "\") in the "
                                    + declaringClass + " class but there is no cache with this name in the application");
                }
            } else if (target.kind() == Kind.METHOD) {
                ClassInfo declaringClass = target.asMethod().declaringClass();
                if (!CACHE_PRODUCER.equals(declaringClass.name())) {
                    throw new DeploymentException(
                            "The @CacheName annotation is not allowed on a method: [class= "
                                    + declaringClass + ", method= " + target.asMethod().name() + "]");
                }
            }
        }
    }
}
