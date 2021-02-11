package io.quarkus.arc.processor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

final class IndexClassLookupUtils {

    private static final Logger LOGGER = Logger.getLogger(IndexClassLookupUtils.class);

    // set of already encountered and logged DotNames that are missing in the index
    private static final Set<DotName> alreadyKnown = ConcurrentHashMap.newKeySet();

    private IndexClassLookupUtils() {
    }

    /**
     * 
     * @param index
     * @param type
     * @return the class for the given type or {@code null} for primitives, arrays and
     */
    static ClassInfo getClassByName(IndexView index, Type type) {
        if (type != null && (type.kind() == Kind.CLASS || type.kind() == Kind.PARAMETERIZED_TYPE)) {
            return getClassByName(index, type.name());
        }
        return null;
    }

    static ClassInfo getClassByName(IndexView index, DotName dotName) {
        return getClassByName(index, dotName, true);
    }

    static ClassInfo getClassByName(IndexView index, DotName dotName, boolean withLogging) {
        if (dotName == null) {
            throw new IllegalArgumentException("Cannot lookup class, provided DotName was null.");
        }
        if (index == null) {
            throw new IllegalArgumentException("Cannot lookup class, provided Jandex Index was null.");
        }
        ClassInfo info = index.getClassByName(dotName);
        if (info == null && withLogging && !alreadyKnown.contains(dotName)) {
            // class not in index, log info as this may cause the application to blow up or behave weirdly
            LOGGER.infof("Class for name: %s was not found in Jandex index. Please ensure the class " +
                    "is part of the index.", dotName);
            alreadyKnown.add(dotName);
        }
        return info;
    }
}
