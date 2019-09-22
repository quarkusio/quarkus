package io.quarkus.vertx.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.deployment.FilterBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FilterBuildItemTest {

    @Test
    public void testComparator() {
        Handler<RoutingContext> noop = rc -> {
            // Do nothing.
        };

        List<FilterBuildItem> list = Arrays.asList(
                new FilterBuildItem(noop, 10),
                new FilterBuildItem(noop, 5),
                new FilterBuildItem(noop, 100));

        Collections.sort(list);

        Assertions.assertEquals(100, list.get(0).getPriority());
        Assertions.assertEquals(10, list.get(1).getPriority());
        Assertions.assertEquals(5, list.get(2).getPriority());
    }

}
