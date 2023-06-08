package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.SemanticException;

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
        if (!isNamedQuery(entityClass, namedQuery)) {
            throw new PanacheQueryException("The named query '" + namedQuery +
                    "' must be defined on your JPA entity or one of its super classes");
        }
    }

    public static boolean isNamedQuery(Class<?> entityClass, String namedQuery) {
        Set<String> namedQueries = namedQueryMap.get(entityClass.getName());
        return namedQueries != null && namedQueries.contains(namedQuery);
    }

    private static boolean isNamedQuery(String namedQuery) {
        for (Set<String> namedQueries : namedQueryMap.values()) {
            if (namedQueries.contains(namedQuery)) {
                return true;
            }
        }
        return false;
    }

    public static RuntimeException checkForNamedQueryMistake(IllegalArgumentException x, String originalQuery) {
        if (originalQuery != null
                && x.getCause() instanceof SemanticException
                && isNamedQuery(originalQuery)) {
            return new PanacheQueryException("Invalid query '" + originalQuery
                    + "' but it matches a known @NamedQuery, perhaps you should prefix it with a '#' to use it as a named query: '#"
                    + originalQuery + "'", x);
        } else {
            return x;
        }
    }
}
