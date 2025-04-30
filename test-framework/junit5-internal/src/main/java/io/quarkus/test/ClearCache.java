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
        clearResourceManagerPropertiesCache();
        clearBeansIntrospectorCache();
    }

    private static void clearAnnotationCache() {
        clearMap(AnnotationUtils.class, "repeatableAnnotationContainerCache");
    }

    /**
     * This will only be effective if the tests are launched with --add-opens java.naming/com.sun.naming.internal=ALL-UNNAMED,
     * which is the case in the Quarkus codebase where memory usage is actually an issue.
     * <p>
     * While not mandatory, this actually helps so enabling it only in the Quarkus codebase has actual value.
     */
    private static void clearResourceManagerPropertiesCache() {
        try {
            clearMap(Class.forName("com.sun.naming.internal.ResourceManager"), "propertiesCache");
        } catch (ClassNotFoundException e) {
            // ignore
            log.debug("Unable to load com.sun.naming.internal.ResourceManager", e);
        }
    }

    private static void clearBeansIntrospectorCache() {
        try {
            Introspector.flushCaches();
        } catch (Exception e) {
            // ignore
            log.debug("Failed to clear java.beans.Introspector cache", e);
        }
    }

    private static void clearMap(Class<?> clazz, String mapField) {
        try {
            Field f = clazz.getDeclaredField(mapField);
            f.setAccessible(true);
            ((Map) (f.get(null))).clear();
        } catch (Exception e) {
            // ignore
            log.debugf(e, "Failed to clear cache for %s#%s cache", clazz.getName(), mapField);
        }
    }
}
