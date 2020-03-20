package io.quarkus.hibernate.orm.panache.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Sort;

public class JpaOperationsSortTest {

    @Test
    public void testSortBy() {
        Sort sort = Sort.by("foo", "_bar");
        assertEquals(" ORDER BY foo , _bar", JpaOperations.toOrderBy(sort));
    }

    @Test
    public void testInvalidSortBy() {
        assertThrows(IllegalArgumentException.class, () -> Sort.by("foo;", "bar"));
    }

    @Test
    public void testEmptySortByYieldsEmptyString() {
        Sort emptySort = Sort.by();
        assertEquals("", JpaOperations.toOrderBy(emptySort));
    }

}
