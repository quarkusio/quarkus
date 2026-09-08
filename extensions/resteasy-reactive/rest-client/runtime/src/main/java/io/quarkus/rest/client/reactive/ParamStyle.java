package io.quarkus.rest.client.reactive;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;

/**
 * Determines how multiple values of the same query parameter or {@code application/x-www-form-urlencoded} form
 * parameter are sent.
 * <p>
 * For query parameters, this is the equivalent of the MicroProfile REST Client {@link QueryParamStyle}.
 */
public enum ParamStyle {
    /**
     * Multiple key/value pairs, each with the same key: {@code foo=v1&foo=v2&foo=v3}
     */
    MULTI_PAIRS,
    /**
     * A single key/value pair, with the values separated by commas: {@code foo=v1,v2,v3}
     */
    COMMA_SEPARATED,
    /**
     * Multiple key/value pairs, with {@code []} appended to the key: {@code foo[]=v1&foo[]=v2&foo[]=v3}
     */
    ARRAY_PAIRS;

    public static ParamStyle from(QueryParamStyle queryParamStyle) {
        if (queryParamStyle == null) {
            return null;
        }
        return switch (queryParamStyle) {
            case MULTI_PAIRS -> MULTI_PAIRS;
            case COMMA_SEPARATED -> COMMA_SEPARATED;
            case ARRAY_PAIRS -> ARRAY_PAIRS;
        };
    }
}
