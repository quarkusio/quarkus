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
