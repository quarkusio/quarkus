package io.quarkus.spring.cache;

import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;

final class SpringCacheUtil {

    private SpringCacheUtil() {
    }

    /**
     * Meant to be called for instances of {@code @Cacheable}, {@code @CacheEvict} or {@code @CachePut}
     * Returns the single name of the cache to use
     */
    static Optional<String> getSpringCacheName(AnnotationInstance annotationInstance) {
        String cacheName = null;
        AnnotationValue cacheNameValue = annotationInstance.value("cacheNames");
        if (cacheNameValue != null) {
            cacheName = singleName(cacheNameValue, annotationInstance.target());
        }
        if (cacheName == null || cacheName.isEmpty()) {
            AnnotationValue value = annotationInstance.value();
            if (value != null) {
                cacheName = singleName(value, annotationInstance.target());
            }
        }
        return (cacheName == null || cacheName.isEmpty()) ? Optional.empty() : Optional.of(cacheName);
    }

    private static String singleName(AnnotationValue annotationValue, AnnotationTarget target) {
        if (annotationValue.kind() != AnnotationValue.Kind.ARRAY) { // shouldn't happen
            return null;
        }

        String[] strings = annotationValue.asStringArray();
        if (strings.length == 0) {
            return null;
        } else if (strings.length > 1) {
            throw new IllegalArgumentException(
                    String.format("Quarkus currently only supports using a single cache name. Offending %s is %s",
                            target.kind() == AnnotationTarget.Kind.METHOD ? "method" : "class", target.toString()));
        }
        return strings[0];
    }
}
