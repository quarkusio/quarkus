package io.quarkus.spring.data.runtime;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.panache.common.Sort;

public final class TypesConverter {

    private TypesConverter() {
    }

    public static io.quarkus.panache.common.Sort toPanacheSort(org.springframework.data.domain.Sort sort) {
        List<org.springframework.data.domain.Sort.Order> orders = sort.get().collect(Collectors.toList());
        if (orders.isEmpty()) {
            return null;
        }

        org.springframework.data.domain.Sort.Order firstOrder = orders.get(0);
        Sort result = Sort.by(firstOrder.getProperty(), getDirection(firstOrder));
        if (orders.size() == 1) {
            return result;
        }

        for (int i = 1; i < orders.size(); i++) {
            org.springframework.data.domain.Sort.Order order = orders.get(i);
            result = result.and(order.getProperty(), getDirection(order));
        }
        return result;
    }

    private static Sort.Direction getDirection(org.springframework.data.domain.Sort.Order order) {
        return order.getDirection() == org.springframework.data.domain.Sort.Direction.ASC ? Sort.Direction.Ascending
                : Sort.Direction.Descending;
    }

    public static io.quarkus.panache.common.Page toPanachePage(org.springframework.data.domain.Pageable pageable) {
        return new io.quarkus.panache.common.Page(pageable.getPageNumber(), pageable.getPageSize());
    }
}
