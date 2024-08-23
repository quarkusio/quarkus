package io.quarkus.spring.data.runtime;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.panache.common.Sort;

@SuppressWarnings("unused") // the methods of this class are invoked in the generated bytecode
public final class TypesConverter {

    private TypesConverter() {
    }

    public static io.quarkus.panache.common.Sort toPanacheSort(org.springframework.data.domain.Sort sort) {
        List<org.springframework.data.domain.Sort.Order> orders = sort.get().collect(Collectors.toList());
        if (orders.isEmpty()) {
            return null;
        }

        org.springframework.data.domain.Sort.Order firstOrder = orders.get(0);
        Sort result = Sort.by(firstOrder.getProperty(), getDirection(firstOrder), getNullPrecedence(firstOrder));
        if (orders.size() == 1) {
            return result;
        }

        for (int i = 1; i < orders.size(); i++) {
            org.springframework.data.domain.Sort.Order order = orders.get(i);
            result = result.and(order.getProperty(), getDirection(order), getNullPrecedence(order));
        }
        return result;
    }

    private static Sort.Direction getDirection(org.springframework.data.domain.Sort.Order order) {
        return order.getDirection() == org.springframework.data.domain.Sort.Direction.ASC ? Sort.Direction.Ascending
                : Sort.Direction.Descending;
    }

    private static Sort.NullPrecedence getNullPrecedence(org.springframework.data.domain.Sort.Order order) {
        if (order.getNullHandling() != null) {
            switch (order.getNullHandling()) {
                case NULLS_FIRST:
                    return Sort.NullPrecedence.NULLS_FIRST;
                case NULLS_LAST:
                    return Sort.NullPrecedence.NULLS_LAST;
            }
        }

        return null;
    }

    public static io.quarkus.panache.common.Page toPanachePage(org.springframework.data.domain.Pageable pageable) {
        // only generate queries with paging if param is actually paged (ex. Unpaged.INSTANCE is a Pageable not paged)
        if (pageable.isPaged()) {
            int pageNumber = pageable.getPageNumber();
            int pageSize = pageable.getPageSize();
            return new io.quarkus.panache.common.Page(pageNumber, pageSize);
        }
        return null; //PanacheQueryImpl#list properly handles null
    }

    public static io.quarkus.panache.common.Sort pageToPanacheSort(org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            return toPanacheSort(sort);
        }
        return null;
    }
}
