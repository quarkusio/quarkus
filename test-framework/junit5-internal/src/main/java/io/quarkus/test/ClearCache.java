package io.quarkus.test;

import java.lang.reflect.Field;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.platform.commons.util.AnnotationUtils;

public class ClearCache {

    private static final Logger log = Logger.getLogger(ClearCache.class);

    public static void clearAnnotationCache() {
        try {
            Field f = AnnotationUtils.class.getDeclaredField("repeatableAnnotationContainerCache");
            f.setAccessible(true);
            ((Map) (f.get(null))).clear();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //ignore
            log.debug("Failed to clear cache", e);
        }
    }
}
