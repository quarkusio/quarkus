package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.exception.PanacheQueryException;
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
        assertEquals(" ORDER BY `foo` , `bar`", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testEmptySortEmptyYieldsEmptyString() {
        Sort emptySort = Sort.empty();
        assertEquals("", PanacheJpaUtil.toOrderBy(emptySort));
    }

    @Test
    public void testSortByNullsFirst() {
        Sort sort = Sort.by("foo", Sort.Direction.Ascending, Sort.NullPrecedence.NULLS_FIRST);
        assertEquals(" ORDER BY `foo` NULLS FIRST", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testSortByNullsLast() {
        Sort sort = Sort.by("foo", Sort.Direction.Descending, Sort.NullPrecedence.NULLS_LAST);
        assertEquals(" ORDER BY `foo` DESC NULLS LAST", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testSortByColumnWithBacktick() {
        Sort sort = Sort.by("jeanne", "d`arc");
        Assertions.assertThrowsExactly(PanacheQueryException.class, () -> PanacheJpaUtil.toOrderBy(sort),
                "Sort column name cannot have backticks");
    }

    @Test
    public void testSortByQuotedColumn() {
        Sort sort = Sort.by("`foo`", "bar");
        assertEquals(" ORDER BY `foo` , `bar`", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testSortByEmbeddedColumn() {
        Sort sort = Sort.by("foo.bar");
        assertEquals(" ORDER BY `foo`.`bar`", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testSortByQuotedEmbeddedColumn() {
        Sort sort1 = Sort.by("foo.`bar`");
        assertEquals(" ORDER BY `foo`.`bar`", PanacheJpaUtil.toOrderBy(sort1));
        Sort sort2 = Sort.by("`foo`.bar");
        assertEquals(" ORDER BY `foo`.`bar`", PanacheJpaUtil.toOrderBy(sort2));
        Sort sort3 = Sort.by("`foo`.`bar`");
        assertEquals(" ORDER BY `foo`.`bar`", PanacheJpaUtil.toOrderBy(sort3));
    }

    @Test
    public void testSortByDisabledEscaping() {
        Sort sort1 = Sort.by("foo.`bar`").disableEscaping();
        assertEquals(" ORDER BY foo.`bar`", PanacheJpaUtil.toOrderBy(sort1));
    }

    @Test
    public void testCaseInsensitiveSorting() {
        Sort sort = Sort.ascendingIgnoreCase("name");
        assertEquals(" ORDER BY LOWER(`name`)", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testCaseInsensitiveSortingDescending() {
        Sort sort = Sort.descendingIgnoreCase("name");
        assertEquals(" ORDER BY LOWER(`name`) DESC", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testCaseInsensitiveSortingWithNullPrecedence() {
        Sort sort = Sort.ascendingIgnoreCase("name").nullsFirst();
        assertEquals(" ORDER BY LOWER(`name`) NULLS FIRST", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testMixedCaseSensitiveAndInsensitive() {
        Sort sort = Sort.by("category").andIgnoreCase("name", Sort.Direction.Descending);
        assertEquals(" ORDER BY `category` , LOWER(`name`) DESC", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testCaseInsensitiveEmbeddedColumn() {
        Sort sort = Sort.ascendingIgnoreCase("author.name");
        assertEquals(" ORDER BY LOWER(`author`.`name`)", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testCaseInsensitiveDisabledEscaping() {
        Sort sort = Sort.ascendingIgnoreCase("name").disableEscaping();
        assertEquals(" ORDER BY LOWER(name)", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testIgnoreCaseFluentAPI() {
        Sort sort = Sort.by("name", "author").ignoreCase();
        assertEquals(" ORDER BY LOWER(`name`) , LOWER(`author`)", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testCaseInsensitiveMultipleColumns() {
        Sort sort = Sort.ascendingIgnoreCase("name", "author");
        assertEquals(" ORDER BY LOWER(`name`) , LOWER(`author`)", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testNullsFirstConvenience() {
        Sort sort = Sort.by("foo").nullsFirst();
        assertEquals(" ORDER BY `foo` NULLS FIRST", PanacheJpaUtil.toOrderBy(sort));
    }

    @Test
    public void testNullsLastConvenience() {
        Sort sort = Sort.by("foo").nullsLast();
        assertEquals(" ORDER BY `foo` NULLS LAST", PanacheJpaUtil.toOrderBy(sort));
    }
}
