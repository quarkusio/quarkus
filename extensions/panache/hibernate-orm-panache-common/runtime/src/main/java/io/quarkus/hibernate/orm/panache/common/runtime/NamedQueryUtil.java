package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.panache.common.exception.PanacheQueryException;

public final class NamedQueryUtil {

    // will be replaced at augmentation phase
    private static volatile Map<String, Set<String>> namedQueryMap = Collections.emptyMap();

    private NamedQueryUtil() {
        // prevent initialization
    }

    public static void setNamedQueryMap(Map<String, Set<String>> newNamedQueryMap) {
        namedQueryMap = newNamedQueryMap;
    }

    public static void checkNamedQuery(Class<?> entityClass, String namedQuery) {
        Set<String> namedQueries = namedQueryMap.get(entityClass.getName());
        if (namedQueries == null || !namedQueries.contains(namedQuery)) {
            throw new PanacheQueryException("The named query '" + namedQuery +
                    "' must be defined on your JPA entity or one of its super classes");
        }
    }
}
