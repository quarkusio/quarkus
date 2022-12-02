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
    public void testEmptySortEmptyYieldsEmptyString() {
        Sort emptySort = Sort.empty();
        assertEquals("", PanacheJpaUtil.toOrderBy(emptySort));
    }

    @Test
    public void testSortByNullsFirst() {
        Sort emptySort = Sort.by("foo", Sort.Direction.Ascending, Sort.NullPrecedence.NULLS_FIRST);
        assertEquals(" ORDER BY foo NULLS FIRST", PanacheJpaUtil.toOrderBy(emptySort));
    }

    @Test
    public void testSortByNullsLast() {
        Sort emptySort = Sort.by("foo", Sort.Direction.Descending, Sort.NullPrecedence.NULLS_LAST);
        assertEquals(" ORDER BY foo DESC NULLS LAST", PanacheJpaUtil.toOrderBy(emptySort));
    }

}
