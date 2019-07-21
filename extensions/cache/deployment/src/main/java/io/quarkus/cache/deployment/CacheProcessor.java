package io.quarkus.cache.deployment;

import static io.quarkus.cache.deployment.CacheDeploymentConstants.ALL_CACHE_ANNOTATIONS;
import static io.quarkus.cache.deployment.CacheDeploymentConstants.CACHE_NAME_PARAMETER_NAME;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.jboss.jandex.AnnotationTarget.Kind.METHOD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension.Key;
import io.quarkus.cache.runtime.augmented.AugmentedCacheInvalidateAllInterceptor;
import io.quarkus.cache.runtime.augmented.AugmentedCacheInvalidateInterceptor;
import io.quarkus.cache.runtime.augmented.AugmentedCacheResultInterceptor;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheBuildRecorder;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheInfo;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class CacheProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CACHE);
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationsTransformer() {
        return new AnnotationsTransformerBuildItem(new CacheAnnotationsTransformer());
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        return Arrays.asList(
                new AdditionalBeanBuildItem(AugmentedCacheInvalidateAllInterceptor.class),
                new AdditionalBeanBuildItem(AugmentedCacheInvalidateInterceptor.class),
                new AdditionalBeanBuildItem(AugmentedCacheResultInterceptor.class));
    }

    @BuildStep
    ValidationErrorBuildItem validateBeanDeployment(ValidationPhaseBuildItem validationPhase) {
        AnnotationStore annotationStore = validationPhase.getContext().get(Key.ANNOTATION_STORE);
        List<Throwable> throwables = new ArrayList<>();
        for (BeanInfo bean : validationPhase.getContext().get(Key.BEANS)) {
            if (bean.isClassBean()) {
                for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                    if (annotationStore.hasAnyAnnotation(method, ALL_CACHE_ANNOTATIONS)) {
                        CacheMethodValidator.validateAnnotations(annotationStore, bean, method, throwables);
                    }
                }
            }
        }
        return new ValidationErrorBuildItem(throwables.toArray(new Throwable[throwables.size()]));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void recordCachesBuild(CombinedIndexBuildItem combinedIndex, BeanContainerBuildItem beanContainer, CacheConfig config,
            CaffeineCacheBuildRecorder caffeineRecorder) {
        Set<String> cacheNames = getCacheNames(combinedIndex.getIndex());
        switch (config.type) {
            case CacheDeploymentConstants.CAFFEINE_CACHE_TYPE:
                Set<CaffeineCacheInfo> cacheInfos = CaffeineCacheInfoBuilder.build(cacheNames, config);
                caffeineRecorder.buildCaches(beanContainer.getValue(), cacheInfos);
                break;
            default:
                throw new DeploymentException("Unknown cache type: " + config.type);
        }
    }

    private Set<String> getCacheNames(IndexView index) {
        Set<String> cacheNames = new HashSet<>();
        for (DotName cacheAnnotation : ALL_CACHE_ANNOTATIONS) {
            for (AnnotationInstance annotation : index.getAnnotations(cacheAnnotation)) {
                if (annotation.target().kind() == METHOD) {
                    cacheNames.add(annotation.value(CACHE_NAME_PARAMETER_NAME).asString());
                }
            }
        }
        return cacheNames;
    }
}
