package io.quarkus.test;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.platform.commons.util.AnnotationUtils;

public class ClearCache {

    private static final Logger log = Logger.getLogger(ClearCache.class);

    public static void clearCaches() {
        clearAnnotationCache();
        clearBeansIntrospectorCache();
    }

    private static void clearAnnotationCache() {
        try {
            Field f = AnnotationUtils.class.getDeclaredField("repeatableAnnotationContainerCache");
            f.setAccessible(true);
            ((Map) (f.get(null))).clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //ignore
            log.debug("Failed to clear annotation cache", e);
        }
    }

    private static void clearBeansIntrospectorCache() {
        try {
            Introspector.flushCaches();
        } catch (Exception e) {
            //ignore
            log.debug("Failed to clear java.beans.Introspector cache", e);
        }
    }
}
