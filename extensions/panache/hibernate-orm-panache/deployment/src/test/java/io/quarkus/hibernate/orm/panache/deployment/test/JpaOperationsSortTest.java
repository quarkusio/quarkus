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
}
