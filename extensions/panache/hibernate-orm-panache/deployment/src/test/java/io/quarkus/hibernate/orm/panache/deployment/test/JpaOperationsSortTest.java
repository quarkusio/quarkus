package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public class JpaOperationsSortTest {

    @Test
    public void testEmptySortByYieldsEmptyString() {
        Sort emptySort = Sort.by();
        assertEquals("", PanacheJpaUtil.toOrderBy(emptySort));
    }

    @Test
    public void testSortBy() {
        Sort sort = Sort.by("foo", "bar");
        assertEquals(" ORDER BY foo , bar", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testSortEmtyYieldsEmptyString() {
        Sort emptySort = Sort.empty();
        assertEquals("", PanacheJpaUtil.toOrderBy(emptySort));
    }

}
