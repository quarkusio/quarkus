package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.data.Sort;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a single {@link Sort} criterion from an HTTP query parameter.
 * <p>
 * <b>Parameter naming:</b>
 * <ul>
 * <li><b>Default name:</b> {@code "sort"} when not annotated</li>
 * <li><b>Custom name:</b> Uses the value specified in {@code @RestQuery("customName")} or
 * {@code @QueryParam("customName")}</li>
 * <li><b>Inferred name:</b> Uses the Java parameter name when bare {@code @RestQuery} or
 * {@code @RestQuery("")} is used</li>
 * </ul>
 * <p>
 * Query parameter value: property name to sort by; prefix with {@code -} for descending
 * (default: {@code null})
 * <p>
 * <b>Examples:</b>
 *
 * <pre>
 * {@code Sort<?> sort}  →  ?sort=name (ascending)
 * {@code @RestQuery("sortBy") Sort<?> customSort}  →  ?sortBy=-salary (descending)
 * {@code @RestQuery Sort<?> sortCriteria}  →  ?sortCriteria=age
 * </pre>
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
