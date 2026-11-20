package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.List;

import jakarta.data.Order;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts an {@link Order} (multiple sort criteria) from repeating HTTP query parameter.
 * <p>
 * <b>Parameter naming:</b>
 * <ul>
 * <li><b>Default name:</b> {@code "sort"} when not annotated or using bare {@code @RestQuery}</li>
 * <li><b>Custom name:</b> Uses the value specified in {@code @RestQuery("customName")}</li>
 * <li><b>Inferred name:</b> Uses the Java parameter name when {@code @RestQuery} or
 * {@code @RestQuery("")} is used</li>
 * </ul>
 * <p>
 * Query parameter value: property name to sort by; prefix with {@code -} for descending.
 * Repeat for multiple criteria. (default: empty {@code Order})
 * <p>
 * <b>Examples:</b>
 *
 * <pre>
 * {@code Order<?> order}  →  ?sort=name&sort=-salary
 * {@code @RestQuery("orderBy") Order<?> orderBy}  →  ?orderBy=name&orderBy=-age
 * {@code @RestQuery Order<?> orderCriteria}  →  ?orderCriteria=dept&orderCriteria=-salary
 * </pre>
 */
public class OrderParamExtractor implements ParameterExtractor {

    public final String parameterName;

    public OrderParamExtractor(String parameterName) {
        this.parameterName = parameterName;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        List<String> values = (List<String>) context.getQueryParameter(parameterName, false, false);
        if (values == null || values.isEmpty()) {
            return Order.by();
        }
        return Order.by((List) SortParamExtractor.parseSorts(values));
    }
}
