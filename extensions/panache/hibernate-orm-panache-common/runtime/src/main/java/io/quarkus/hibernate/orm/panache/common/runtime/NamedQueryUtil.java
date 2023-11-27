package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.Collections;
import java.util.Map;

import org.hibernate.query.SemanticException;
import org.hibernate.query.SyntaxException;
import org.hibernate.query.sqm.ParsingException;

import io.quarkus.panache.common.exception.PanacheQueryException;

public final class NamedQueryUtil {

    // will be replaced at augmentation phase
    private static volatile Map<String, Map<String, String>> namedQueryMap = Collections.emptyMap();

    private NamedQueryUtil() {
        // prevent initialization
    }

    public static void setNamedQueryMap(Map<String, Map<String, String>> newNamedQueryMap) {
        namedQueryMap = newNamedQueryMap;
    }

    public static void checkNamedQuery(Class<?> entityClass, String namedQuery) {
        if (!isNamedQuery(entityClass, namedQuery)) {
            throw new PanacheQueryException("The named query '" + namedQuery +
                    "' must be defined on your JPA entity or one of its super classes");
        }
    }

    public static boolean isNamedQuery(Class<?> entityClass, String namedQuery) {
        Map<String, String> namedQueries = namedQueryMap.get(entityClass.getName());
        return namedQueries != null && namedQueries.containsKey(namedQuery);
    }

    private static boolean isNamedQuery(String namedQuery) {
        for (Map<String, String> namedQueries : namedQueryMap.values()) {
            if (namedQueries.containsKey(namedQuery)) {
                return true;
            }
        }
        return false;
    }

    public static RuntimeException checkForNamedQueryMistake(IllegalArgumentException x, String originalQuery) {
        if (originalQuery != null
                && (x.getCause() instanceof SemanticException || x.getCause() instanceof ParsingException
                        || x.getCause() instanceof SyntaxException)
                && isNamedQuery(originalQuery)) {
            return new PanacheQueryException("Invalid query '" + originalQuery
                    + "' but it matches a known @NamedQuery, perhaps you should prefix it with a '#' to use it as a named query: '#"
                    + originalQuery + "'", x);
        } else {
            return x;
        }
    }
}
