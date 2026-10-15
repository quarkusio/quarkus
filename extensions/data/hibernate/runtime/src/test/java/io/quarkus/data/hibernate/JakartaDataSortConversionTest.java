package io.quarkus.data.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.data.Order;
import jakarta.data.Sort;

import org.junit.jupiter.api.Test;

import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public class JakartaDataSortConversionTest {

    private static final class Cat {
    }

    @Test
    public void testToOrderNull() {
        assertNull(PanacheJpaUtil.toOrder(null));
    }

    @Test
    public void testToOrderFromSort() {
        Order<?> order = PanacheJpaUtil.toOrder(Sort.<Cat> asc("name"));
        io.quarkus.panache.common.Sort panacheSort = PanacheJpaUtil.toSort(order);

        assertEquals(1, panacheSort.getColumns().size());
        assertEquals(" ORDER BY `name`", PanacheJpaUtil.toOrderBy(panacheSort));
    }

    @Test
    public void testJakartaDataIgnoreCasePreserved() {
        Order<? super Cat> order = Order.by(Sort.<Cat> ascIgnoreCase("name"));
        io.quarkus.panache.common.Sort panacheSort = PanacheJpaUtil.toSort(order);

        assertEquals(1, panacheSort.getColumns().size());
        assertTrue(panacheSort.getColumns().get(0).isIgnoreCase());
        assertEquals(" ORDER BY LOWER(`name`)", PanacheJpaUtil.toOrderBy(panacheSort));
    }

    @Test
    public void testJakartaDataMixedIgnoreCase() {
        Order<? super Cat> order = Order.by(
                Sort.<Cat> asc("id"),
                Sort.<Cat> descIgnoreCase("name"));
        io.quarkus.panache.common.Sort panacheSort = PanacheJpaUtil.toSort(order);

        assertEquals(2, panacheSort.getColumns().size());
        assertFalse(panacheSort.getColumns().get(0).isIgnoreCase());
        assertTrue(panacheSort.getColumns().get(1).isIgnoreCase());
        assertEquals(" ORDER BY `id` , LOWER(`name`) DESC", PanacheJpaUtil.toOrderBy(panacheSort));
    }

    @Test
    public void testJakartaDataCaseSensitiveNotAffected() {
        Order<? super Cat> order = Order.by(Sort.<Cat> asc("name"));
        io.quarkus.panache.common.Sort panacheSort = PanacheJpaUtil.toSort(order);

        assertEquals(1, panacheSort.getColumns().size());
        assertFalse(panacheSort.getColumns().get(0).isIgnoreCase());
        assertEquals(" ORDER BY `name`", PanacheJpaUtil.toOrderBy(panacheSort));
    }
}
