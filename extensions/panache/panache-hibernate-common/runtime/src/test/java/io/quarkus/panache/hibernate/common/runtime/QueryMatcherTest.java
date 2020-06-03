package io.quarkus.panache.hibernate.common.runtime;

import java.util.regex.Matcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueryMatcherTest {

    @Test
    public void testSelectMatcher() {
        testSelectMatch("from bar", null);
        testSelectMatch("select foo from bar", "from bar");
        testSelectMatch("  select   foo   from   bar  ", "from   bar  ");
        testSelectMatch("select foo as fff from bar", "from bar");
        testSelectMatch("select foo,bar from bar", "from bar");
        testSelectMatch("select foo , bar from bar", "from bar");
        testSelectMatch("select foo as f,bar as b from bar", "from bar");
        testSelectMatch("select foo as f , bar as b from bar", "from bar");
        testSelectMatch("select foo.bar.gee as f , bar.sup as b from bar", "from bar");
        testSelectMatch("select distinct foo from bar \n where field = :value",
                "from bar \n where field = :value");
        testSelectMatch("select distinct foo from bar", "from bar");
        testSelectMatch("select distinct foo,bar from bar", "from bar");
        testSelectMatch("select foo from bar where field = :value", "from bar where field = :value");
        testSelectMatch("select foo from bar \n where field = :value",
                "from bar \n where field = :value");
        testSelectMatch("select foo from bar \n where field = :value " +
                "\n\r and fieldTwo :valueTwo \r\n :fieldThree = :valueThree",
                "from bar \n where field = :value \n\r and fieldTwo :valueTwo \r\n :fieldThree = :valueThree");
    }

    private void testSelectMatch(String select, String result) {
        Matcher matcher = PanacheJpaUtil.SELECT_PATTERN.matcher(select);
        if (result != null) {
            Assertions.assertTrue(matcher.matches());
            Assertions.assertEquals(result, matcher.group(3));
        } else {
            Assertions.assertFalse(matcher.matches());
        }
    }

    @Test
    public void testFromMatcher() {
        testFromMatch("from bar");
        testFromMatch("   from   bar  ");
        testFromMatch("from bar where field = :value");
        testFromMatch("from bar where fieldOne = :valueOne and fieldTwo :valueTwo");
        testFromMatch("from bar where fieldOne = :valueOne \n and fieldTwo :valueTwo");
        testFromMatch("from bar where fieldOne = :valueOne \n\r and fieldTwo :valueTwo");
        testFromMatch("from bar where fieldOne = :valueOne \r\n and fieldTwo :valueTwo \n" +
                "\n :fieldThree = :valueThree");
        testFromMatch("from \n bar where field = :value");
        testFromMatch("from \n bar \n\r where \r\n field = :value");
        testFromMatch("from \n bar b \n\r where \r\n b.field = :value");
    }

    private void testFromMatch(String query) {
        Matcher matcher = PanacheJpaUtil.FROM_PATTERN.matcher(query);
        Assertions.assertTrue(matcher.matches());
    }

}
