package io.quarkus.panache.hibernate.common.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CountTest {
    @Test
    public void testParser() {
        // one column, order/limit/offset
        assertCountQueryUsingParser("select count( * ) from bar", "select foo from bar order by foo, bar ASC limit 2 offset 3");
        // two columns
        assertCountQueryUsingParser("select count( * ) from bar", "select foo,gee from bar");
        // one column distinct
        assertCountQueryUsingParser("select count( distinct foo ) from bar", "select distinct foo from bar");
        // two columns distinct
        assertCountQueryUsingParser("select count( * ) from ( select distinct foo as __v0, gee as __v1 from bar )",
                "select distinct foo,gee from bar");
        assertCountQueryUsingParser("select count( * ) from ( select distinct foo as __v0, gee as g from bar )",
                "select distinct foo,gee as g from bar");
        // nested order by not touched
        assertCountQueryUsingParser("select count( * ) from ( from entity order by id )",
                "select foo from (from entity order by id) order by foo, bar ASC");
        // what happens to literals?
        assertCountQueryUsingParser("select count( * ) from bar where some = 2 and other = '23'",
                "select foo from bar where some = 2 and other = '23'");
        // fetches are gone
        assertCountQueryUsingParser("select count( * ) from bar b", "select foo from bar b left join fetch b.things");
        // non-fetches remain
        assertCountQueryUsingParser("select count( * ) from bar b left join b.things",
                "select foo from bar b left join b.things");

        // inverted select
        assertCountQueryUsingParser("from bar select count( * )", "from bar select foo");
        // from without select
        assertCountQueryUsingParser("from bar select count( * )", "from bar");

        // CTE
        assertFastCountQuery("WITH id AS ( SELECT p.id AS pid FROM Person2 AS p ) SELECT count( * ) FROM Person2 p",
                "WITH id AS (SELECT p.id AS pid FROM Person2 AS p) SELECT p FROM Person2 p");
    }

    @Test
    public void testFastVersion() {
        // one column, order/limit/offset
        assertFastCountQuery("SELECT COUNT(*) from bar", "select foo from bar order by foo, bar ASC limit 2 offset 3");
        // two columns
        assertFastCountQuery("SELECT COUNT(*) from bar", "select foo,gee from bar");
        // one column distinct
        assertFastCountQuery("SELECT COUNT(distinct foo) from bar", "select distinct foo from bar");
        // two columns distinct
        Assertions.assertThrows(RuntimeException.class, () -> assertFastCountQuery("XX", "select distinct foo,gee from bar"));
        // nested order by not touched
        assertFastCountQuery("SELECT COUNT(*) from (from entity order by id)",
                "select foo from (from entity order by id) order by foo, bar ASC");
        // what happens to literals?
        assertFastCountQuery("SELECT COUNT(*) from bar where some = 2 and other = '23'",
                "select foo from bar where some = 2 and other = '23'");
        // fetches are gone
        assertFastCountQuery("select count( * ) from bar b", "select foo from bar b left join fetch b.things");
        // non-fetches remain
        assertFastCountQuery("SELECT COUNT(*) from bar b left join b.things", "select foo from bar b left join b.things");

        // inverted select
        assertFastCountQuery("from bar select count( * )", "from bar select foo");
        // from without select
        assertFastCountQuery("SELECT COUNT(*) from bar", "from bar");

        // CTE
        assertFastCountQuery("WITH id AS ( SELECT p.id AS pid FROM Person2 AS p ) SELECT count( * ) FROM Person2 p",
                "WITH id AS (SELECT p.id AS pid FROM Person2 AS p) SELECT p FROM Person2 p");
    }

    private void assertCountQueryUsingParser(String expected, String selectQuery) {
        String countQuery = PanacheJpaUtil.getCountQueryUsingParser(selectQuery);
        Assertions.assertEquals(expected, countQuery);
    }

    private void assertFastCountQuery(String expected, String selectQuery) {
        String countQuery = PanacheJpaUtil.getFastCountQuery(selectQuery);
        Assertions.assertEquals(expected, countQuery);
    }
}
