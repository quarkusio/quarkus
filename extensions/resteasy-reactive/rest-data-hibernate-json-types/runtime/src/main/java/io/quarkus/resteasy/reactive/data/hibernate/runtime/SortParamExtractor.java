package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.data.Sort;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a single {@link Sort} criterion from an HTTP query parameter.
 * <ul>
 * <li>Query parameter — property name to sort by; prefix with {@code -} for descending (default: {@code null})</li>
 * </ul>
 * Examples: {@code ?sort=name} produces {@code Sort.asc("name")},
 * {@code ?sort=-salary} produces {@code Sort.desc("salary")}.
 */
public class SortParamExtractor implements ParameterExtractor {

    public final String parameterName;

    public SortParamExtractor(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        String value = (String) context.getQueryParameter(parameterName, true, false);
        if (value == null) {
            return null;
        }
        return parseSort(value);
    }

    static Sort<?> parseSort(String value) {
        if (value.startsWith("-")) {
            return Sort.desc(value.substring(1));
        }
        return Sort.asc(value);
    }

    static List<Sort<?>> parseSorts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<Sort<?>> sorts = new ArrayList<>(values.size());
        for (String value : values) {
            sorts.add(parseSort(value));
        }
        return sorts;
    }
}
