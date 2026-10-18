package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.List;

import jakarta.data.Order;
import jakarta.data.Sort;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts an {@link Order} (multiple sort criteria) from repeating HTTP query parameter {@code sort}.
 * <ul>
 * <li>{@code sort} — property name to sort by; prefix with {@code -} for descending.
 * Repeat for multiple criteria. (default: empty {@code Order})</li>
 * </ul>
 * Example: {@code ?sort=name&sort=-salary} produces
 * {@code Order.by(Sort.asc("name"), Sort.desc("salary"))}.
 */
public class OrderParamExtractor implements ParameterExtractor {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        List<String> values = (List<String>) context.getQueryParameter("sort", false, false);
        if (values == null || values.isEmpty()) {
            return Order.by();
        }
        return Order.by((List) SortParamExtractor.parseSorts(values));
    }
}
