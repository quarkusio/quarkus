package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.regex.Matcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryMatcherTest {

    @Test
    public void testSelectMatcher() {
        testMatch("from bar", null);
        testMatch("select foo from bar", "from bar");
        testMatch("  select   foo   from   bar  ", "from   bar  ");
        testMatch("select foo as fff from bar", "from bar");
        testMatch("select foo,bar from bar", "from bar");
        testMatch("select foo , bar from bar", "from bar");
        testMatch("select foo as f,bar as b from bar", "from bar");
        testMatch("select foo as f , bar as b from bar", "from bar");

        testMatch("select foo.bar.gee as f , bar.sup as b from bar", "from bar");

        testMatch("select distinct foo from bar", "from bar");
        testMatch("select distinct foo,bar from bar", "from bar");
    }

    private void testMatch(String select, String result) {
        Matcher matcher = CommonPanacheQueryImpl.SELECT_PATTERN.matcher(select);
        if (result != null) {
            Assertions.assertTrue(matcher.matches());
            Assertions.assertEquals(result, matcher.group(3));
        } else {
            Assertions.assertFalse(matcher.matches());
        }
    }
}
