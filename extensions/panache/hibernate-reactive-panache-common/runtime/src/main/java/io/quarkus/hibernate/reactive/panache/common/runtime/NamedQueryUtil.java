package io.quarkus.hibernate.reactive.panache.common.runtime;

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

    static String getNamedQuery(String namedQuery) {
        for (Map<String, String> namedQueries : namedQueryMap.values()) {
            String query = namedQueries.get(namedQuery);
            if (query != null) {
                return query;
            }
        }
        return null;
    }

    public static RuntimeException checkForNamedQueryMistake(RuntimeException x, String originalQuery) {
        if (originalQuery != null
                && (isQueryStringException(x)
                        // Some APIs return IllegalArgumentException when the query string is invalid,
                        // which is not helpful and probably a result of trying to match JPA behavior,
                        // but the cause is actually meaningful.
                        || x instanceof IllegalArgumentException && isQueryStringException(x.getCause()))
                && isNamedQuery(originalQuery)) {
            return new PanacheQueryException("Invalid query '" + originalQuery
                    + "' but it matches a known @NamedQuery, perhaps you should prefix it with a '#' to use it as a named query: '#"
                    + originalQuery + "'", x);
        } else {
            return x;
        }
    }

    private static boolean isQueryStringException(Throwable x) {
        return x instanceof SemanticException || x instanceof ParsingException
                || x instanceof SyntaxException;
    }
}
