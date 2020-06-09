package io.quarkus.spring.cache;

import static io.quarkus.spring.cache.SpringCacheUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.cache.deployment.AdditionalCacheNameBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class SpringCacheProcessor {

    static final DotName CACHEABLE = DotName.createSimple(Cacheable.class.getName());
    static final DotName CACHE_PUT = DotName.createSimple(CachePut.class.getName());
    static final DotName CACHE_EVICT = DotName.createSimple(CacheEvict.class.getName());

    private static final List<DotName> CACHE_ANNOTATIONS = Collections
            .unmodifiableList(Arrays.asList(CACHEABLE, CACHE_PUT, CACHE_EVICT));

    // some of these restrictions can probably be lifted by us doing additional work on caching after https://github.com/quarkusio/quarkus/pull/8631 lands
    private static final Set<String> CURRENTLY_UNSUPPORTED_ANNOTATION_VALUES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("key", "keyGenerator", "cacheManager", "cacheResolver", "condition",
                    "unless", "sync", "beforeInvocation")));

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.SPRING_CACHE));
    }

    @BuildStep
    AnnotationsTransformerBuildItem transform() {
        return new AnnotationsTransformerBuildItem(new SpringCacheAnnotationsTransformer());
    }

    @BuildStep
    List<AdditionalCacheNameBuildItem> cacheNames(CombinedIndexBuildItem combinedIndex) {
        Set<String> cacheNames = new HashSet<>();
        for (DotName cacheAnnotation : CACHE_ANNOTATIONS) {
            Collection<AnnotationInstance> instances = combinedIndex.getIndex().getAnnotations(cacheAnnotation);
            for (AnnotationInstance instance : instances) {
                validateUsage(instance);
                Optional<String> cacheName = getSpringCacheName(instance);
                if (cacheName.isPresent()) {
                    cacheNames.add(cacheName.get());
                }
            }
        }
        List<AdditionalCacheNameBuildItem> result = new ArrayList<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            result.add(new AdditionalCacheNameBuildItem(cacheName));
        }
        return result;
    }

    private void validateUsage(AnnotationInstance instance) {
        if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
            throw new IllegalArgumentException(
                    "Currently Spring Cache annotations can only be added to methods. Offending instance is annotation '"
                            + instance + "' on " + instance.target() + "'");
        }
        List<AnnotationValue> values = instance.values();
        List<String> unsupportedValues = new ArrayList<>();
        for (AnnotationValue value : values) {
            if (CURRENTLY_UNSUPPORTED_ANNOTATION_VALUES.contains(value.name())) {
                unsupportedValues.add(value.name());
            }
        }
        if (!unsupportedValues.isEmpty()) {
            throw new IllegalArgumentException("Annotation '" +
                    instance + "' on '" + instance.target()
                    + "' contains the following currently unsupported annotation values: "
                    + String.join(", ", unsupportedValues));
        }
    }

}
