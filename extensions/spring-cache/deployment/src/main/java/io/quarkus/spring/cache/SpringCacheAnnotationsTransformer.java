package io.quarkus.spring.cache;

import static io.quarkus.spring.cache.SpringCacheProcessor.CACHEABLE;
import static io.quarkus.spring.cache.SpringCacheProcessor.CACHE_EVICT;
import static io.quarkus.spring.cache.SpringCacheProcessor.CACHE_PUT;
import static io.quarkus.spring.cache.SpringCacheUtil.getSpringCacheName;

import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.deployment.CacheDeploymentConstants;

public class SpringCacheAnnotationsTransformer implements AnnotationsTransformer {

    private static final Logger LOGGER = Logger.getLogger(SpringCacheAnnotationsTransformer.class);

    private static final DotName CACHE_RESULT_INTERCEPTOR_BINDING = DotName
            .createSimple(CacheResult.class.getName());
    private static final DotName CACHE_INVALIDATE_INTERCEPTOR_BINDING = DotName
            .createSimple(CacheInvalidate.class.getName());
    private static final DotName CACHE_INVALIDATE_ALL_INTERCEPTOR_BINDING = DotName
            .createSimple(CacheInvalidateAll.class.getName());

    @Override
    public boolean appliesTo(AnnotationTarget.Kind kind) {
        return kind == AnnotationTarget.Kind.METHOD;
    }

    @Override
    public void transform(TransformationContext transformationContext) {
        AnnotationTarget target = transformationContext.getTarget();
        if (target.kind() != AnnotationTarget.Kind.METHOD) {
            return;
        }
        MethodInfo methodInfo = target.asMethod();
        if (methodInfo.hasAnnotation(CACHEABLE)) {
            AnnotationInstance cacheable = methodInfo.annotation(CACHEABLE);
            Optional<String> cacheName = getSpringCacheName(cacheable);
            if (cacheName.isPresent()) {
                transformationContext.transform().add(CACHE_RESULT_INTERCEPTOR_BINDING,
                        AnnotationValue.createStringValue(CacheDeploymentConstants.CACHE_NAME_PARAM, cacheName.get())).done();
            } else {
                warnAboutMissingCacheName(cacheable, methodInfo);
            }
        } else if (methodInfo.hasAnnotation(CACHE_EVICT)) {
            AnnotationInstance cacheEvict = methodInfo.annotation(CACHE_EVICT);
            Optional<String> cacheName = getSpringCacheName(cacheEvict);
            if (!cacheName.isPresent()) {
                warnAboutMissingCacheName(cacheEvict, methodInfo);
                return;
            }
            AnnotationValue allEntriesValue = cacheEvict.value("allEntries");
            boolean allEntries = false;
            if (allEntriesValue != null) {
                allEntries = allEntriesValue.asBoolean();
            }
            transformationContext.transform()
                    .add(allEntries ? CACHE_INVALIDATE_ALL_INTERCEPTOR_BINDING
                            : CACHE_INVALIDATE_INTERCEPTOR_BINDING,
                            AnnotationValue.createStringValue(CacheDeploymentConstants.CACHE_NAME_PARAM, cacheName.get()))
                    .done();
        } else if (methodInfo.hasAnnotation(CACHE_PUT)) {
            /*
             * @CachePut is just an operation that overrides the cache entry with the new result so it is
             * equivalent of first invalidating the cache entry and then adding the new result
             */
            AnnotationInstance cachePut = methodInfo.annotation(CACHE_PUT);
            Optional<String> cacheName = getSpringCacheName(cachePut);
            if (cacheName.isPresent()) {
                transformationContext
                        .transform()
                        .add(CACHE_RESULT_INTERCEPTOR_BINDING,
                                AnnotationValue.createStringValue(CacheDeploymentConstants.CACHE_NAME_PARAM, cacheName.get()))
                        .add(CACHE_INVALIDATE_INTERCEPTOR_BINDING,
                                AnnotationValue.createStringValue(CacheDeploymentConstants.CACHE_NAME_PARAM, cacheName.get()))
                        .done();
            } else {
                warnAboutMissingCacheName(cachePut, methodInfo);
            }
        }
    }

    private void warnAboutMissingCacheName(AnnotationInstance instance, MethodInfo methodInfo) {
        LOGGER.warn(instance + " has no specified cache name, so it will be ignored. Offending method is '" + methodInfo.name()
                + "' of class '" + methodInfo.declaringClass().name().toString() + "'");
    }
}
