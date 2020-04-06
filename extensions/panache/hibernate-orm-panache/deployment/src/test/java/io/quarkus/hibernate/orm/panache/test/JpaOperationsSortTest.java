package io.quarkus.hibernate.orm.panache.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Sort;

public class JpaOperationsSortTest {

    @Test
    public void testSortBy() {
        Sort sort = Sort.by("foo", "_bar");
        assertEquals(" ORDER BY 'foo' , '_bar'", JpaOperations.toOrderBy(sort));
    }

    @Test
    public void testInvalidSortByWithQuote() {
        final Sort sort = Sort.by("foo'", "bar");
        assertEquals(" ORDER BY 'foo\\'' , 'bar'", JpaOperations.toOrderBy(sort));
    }

    @Test
    public void testInvalidSortByWithEscapeCharacters() {
        final Sort sort = Sort.by("foo", "bar\\");
        assertEquals(" ORDER BY 'foo' , 'bar\\\\'", JpaOperations.toOrderBy(sort));
    }

    @Test
    public void testEmptySortByYieldsEmptyString() {
        Sort emptySort = Sort.by();
        assertEquals("", JpaOperations.toOrderBy(emptySort));
    }

}
