package io.quarkus.hibernate.panache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.data.Order;

import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public class JakartaDataSortConversionTest {

    @Test
    public void testJakartaDataIgnoreCasePreserved() {
        jakarta.data.Order<?> order = Order.by(jakarta.data.Sort.ascIgnoreCase("name"));
        Sort panacheSort = PanacheJpaUtil.toSort(order);

        assertEquals(1, panacheSort.getColumns().size());
        assertTrue(panacheSort.getColumns().get(0).isIgnoreCase());
        assertEquals(" ORDER BY LOWER(`name`)", PanacheJpaUtil.toOrderBy(panacheSort));
    }

    @Test
    public void testJakartaDataMixedIgnoreCase() {
        jakarta.data.Order<?> order = Order.by(
                jakarta.data.Sort.asc("id"),
                jakarta.data.Sort.descIgnoreCase("name"));
        Sort panacheSort = PanacheJpaUtil.toSort(order);

        assertEquals(2, panacheSort.getColumns().size());
        assertFalse(panacheSort.getColumns().get(0).isIgnoreCase());
        assertTrue(panacheSort.getColumns().get(1).isIgnoreCase());
        assertEquals(" ORDER BY `id` , LOWER(`name`) DESC", PanacheJpaUtil.toOrderBy(panacheSort));
    }

    @Test
    public void testJakartaDataCaseSensitiveNotAffected() {
        jakarta.data.Order<?> order = Order.by(jakarta.data.Sort.asc("name"));
        Sort panacheSort = PanacheJpaUtil.toSort(order);

        assertEquals(1, panacheSort.getColumns().size());
        assertFalse(panacheSort.getColumns().get(0).isIgnoreCase());
        assertEquals(" ORDER BY `name`", PanacheJpaUtil.toOrderBy(panacheSort));
    }
}
