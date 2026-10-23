package io.quarkus.it.rest.data.hibernate;

import java.util.List;
import java.util.Map;

import jakarta.data.Direction;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.impl.PageRecord;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Path("echo")
public class ParamEchoResource {

    @GET
    @Path("page-request")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> pageRequest(PageRequest pageRequest) {
        return Map.of(
                "page", pageRequest.page(),
                "size", pageRequest.size(),
                "requestTotal", pageRequest.requestTotal());
    }

    @GET
    @Path("sort")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> sort(Sort<?> sort) {
        if (sort == null) {
            return null;
        }
        return Map.of(
                "property", sort.property(),
                "ascending", sort.isAscending());
    }

    @GET
    @Path("order")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> order(Order<?> order) {
        List<Map<String, Object>> sorts = order.sorts().stream()
                .map(s -> Map.<String, Object> of(
                        "property", s.property(),
                        "ascending", s.isAscending()))
                .toList();
        return Map.of("sorts", sorts);
    }

    @GET
    @Path("limit")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> limit(Limit limit) {
        if (limit == null) {
            return null;
        }
        return Map.of(
                "maxResults", limit.maxResults(),
                "startAt", limit.startAt());
    }

    @GET
    @Path("direction")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> direction(Direction direction) {
        if (direction == null) {
            return null;
        }
        return Map.of("direction", direction.name());
    }

    @GET
    @Path("page")
    @Produces(MediaType.APPLICATION_JSON)
    public Page<Item> page() {
        List<Item> items = List.of(new Item("a"), new Item("b"), new Item("c"));
        return new PageRecord<>(PageRequest.ofPage(1, 3, true), items, 10);
    }

    @GET
    @Path("page/no-totals")
    @Produces(MediaType.APPLICATION_JSON)
    public Page<Item> pageNoTotals() {
        List<Item> items = List.of(new Item("x"), new Item("y"));
        return new PageRecord<>(PageRequest.ofPage(2, 2, false), items, -1);
    }

    @GET
    @Path("page/annotated")
    @Produces(MediaType.APPLICATION_JSON)
    public Page<AnnotatedItem> pageAnnotated() {
        List<AnnotatedItem> items = List.of(new AnnotatedItem("Alice", 30, "s3cret"));
        return new PageRecord<>(PageRequest.ofPage(1, 10, true), items, 1);
    }

    /**
     * Echoes an Order parameter with a custom parameter name specified via @RestQuery.
     * Accepts query parameters like ?orderBy=name or ?orderBy=-age
     */
    @GET
    @Path("order-custom-name")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> orderWithCustomName(@RestQuery("orderBy") Order<?> customOrder) {
        if (customOrder == null || customOrder.sorts().isEmpty()) {
            return Map.of("message", "No sorting provided");
        }
        List<Map<String, Object>> sorts = customOrder.sorts().stream()
                .map(s -> Map.<String, Object> of(
                        "property", s.property(),
                        "ascending", s.isAscending()))
                .toList();
        return Map.of(
                "parameterName", "orderBy",
                "sorts", sorts);
    }

    /**
     * Echoes a Sort parameter with a custom parameter name specified via @RestQuery.
     * Accepts query parameters like ?sortBy=name or ?sortBy=-age
     */
    @GET
    @Path("sort-custom-name")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> sortWithCustomName(@RestQuery("sortBy") Sort<?> customSort) {
        if (customSort == null) {
            return Map.of("message", "No sorting provided");
        }
        return Map.of(
                "parameterName", "sortBy",
                "property", customSort.property(),
                "ascending", customSort.isAscending());
    }

    /**
     * Echoes an Order parameter with an empty @RestQuery value.
     * Falls back to the default "sort" parameter name.
     */
    @GET
    @Path("empty-query-name")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> testEmptyQueryName(@RestQuery("") Order<?> order) {
        if (order == null || order.sorts().isEmpty()) {
            return Map.of("message", "No sorting provided");
        }
        List<Map<String, Object>> sorts = order.sorts().stream()
                .map(s -> Map.<String, Object> of(
                        "property", s.property(),
                        "ascending", s.isAscending()))
                .toList();
        return Map.of(
                "message", "Empty @RestQuery value",
                "sorts", sorts);
    }

    /**
     * Echoes multiple Jakarta Data parameters with different custom names.
     * Demonstrates that different parameters can use independent custom names.
     */
    @GET
    @Path("multiple-custom-params")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> testMultipleCustomParams(
            @RestQuery("orderBy") Order<?> ordering,
            @RestQuery("filterBy") Sort<?> filter) {
        Map<String, Object> result = new java.util.HashMap<>();

        if (ordering != null && !ordering.sorts().isEmpty()) {
            List<Map<String, Object>> sorts = ordering.sorts().stream()
                    .map(s -> Map.<String, Object> of(
                            "property", s.property(),
                            "ascending", s.isAscending()))
                    .toList();
            result.put("ordering", sorts);
        }

        if (filter != null) {
            result.put("filter", Map.of(
                    "property", filter.property(),
                    "ascending", filter.isAscending()));
        }

        return result;
    }

    /**
     * Echoes Jakarta Data parameters using both default and custom names.
     * The Sort parameter uses the default "sort" name, while Order uses a custom name.
     */
    @GET
    @Path("mixed-params")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> testMixedParams(
            Sort<?> defaultSort,
            @RestQuery("customOrder") Order<?> customOrder) {
        Map<String, Object> result = new java.util.HashMap<>();

        if (defaultSort != null) {
            result.put("defaultSort", Map.of(
                    "property", defaultSort.property(),
                    "ascending", defaultSort.isAscending()));
        }

        if (customOrder != null && !customOrder.sorts().isEmpty()) {
            List<Map<String, Object>> sorts = customOrder.sorts().stream()
                    .map(s -> Map.<String, Object> of(
                            "property", s.property(),
                            "ascending", s.isAscending()))
                    .toList();
            result.put("customOrder", sorts);
        }

        return result;
    }

    /**
     * Echoes a Sort parameter with a custom name containing an underscore.
     * Verifies that parameter names with special characters are supported.
     */
    @GET
    @Path("special-chars")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> testSpecialChars(@RestQuery("order_by") Sort<?> orderBy) {
        if (orderBy == null) {
            return Map.of("message", "No sorting provided");
        }
        return Map.of(
                "parameterName", "order_by",
                "property", orderBy.property(),
                "ascending", orderBy.isAscending());
    }

    public static class Item {
        public String name;

        public Item(String name) {
            this.name = name;
        }
    }

    public static class AnnotatedItem {
        @JsonProperty("full_name")
        public String name;

        public int age;

        @JsonIgnore
        public String secret;

        public AnnotatedItem(String name, int age, String secret) {
            this.name = name;
            this.age = age;
            this.secret = secret;
        }
    }
}
