package io.quarkus.arc.processor;

import java.util.HashSet;
import java.util.Set;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

final class IndexClassLookupUtils {

    private static final Logger LOGGER = Logger.getLogger(IndexClassLookupUtils.class);

    // set of already encountered and logged DotNames that are missing in the index
    private static Set<DotName> alreadyKnown = new HashSet<>();

    private IndexClassLookupUtils() {
    }

    static ClassInfo getClassByName(IndexView index, DotName dotName) {
        return lookupClassInIndex(index, dotName, true);
    }

    /**
     * Used by {@code BeanArchives.IndexWrapper#getClassByName()} while gathering additional classes for indexing
     */
    static ClassInfo getClassByNameNoLogging(IndexView index, DotName dotName) {
        return lookupClassInIndex(index, dotName, false);
    }

    private static ClassInfo lookupClassInIndex(IndexView index, DotName dotName, boolean withLogging) {
        if (dotName == null) {
            throw new IllegalArgumentException("Cannot lookup class, provided DotName was null.");
        }
        if (index == null) {
            throw new IllegalArgumentException("Cannot lookup class, provided Jandex Index was null.");
        }
        ClassInfo info = index.getClassByName(dotName);
        if (info == null && withLogging && !alreadyKnown.contains(dotName)) {
            // class not in index, log info as this may cause the application to blow up or behave weirdly
            LOGGER.info("Class for name: " + dotName + " was not found in Jandex index. Please ensure the class " +
                    "is part of the index.");
            alreadyKnown.add(dotName);
        }
        return info;
    }
}
