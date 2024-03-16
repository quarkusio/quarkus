package io.quarkus.runtime.util;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.join;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.lowerCaseFirst;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests of the {@linkplain StringUtil} methods
 */
public class StringUtilTestCase {

    /**
     * Test word beginning with multiple uppercase letters
     */
    @Test
    public void testHyphenateUppercaseBegin() {
        String hyphenated = StringUtil.hyphenate("SBVbt");
        assertEquals("sb-vbt", hyphenated);
    }

    /**
     * Test word with only uppercase letters
     */
    @Test
    public void testHyphenateAllUppercase() {
        String hyphenated = StringUtil.hyphenate("SHOUT");
        assertEquals("shout", hyphenated);
    }

    /**
     * Test word with multiple uppercase letters in middle
     */
    @Test
    public void testHyphenateUppercaseMiddle() {
        String hyphenated = StringUtil.hyphenate("btSBVsuffix");
        assertEquals("bt-sb-vsuffix", hyphenated);
    }

    /**
     * Test word with multiple uppercase letters at end
     */
    @Test
    public void testHyphenateUppercaseEnd() {
        String hyphenated = StringUtil.hyphenate("btSBV");
        assertEquals("bt-sbv", hyphenated);
    }

    /**
     * Test the special case of a word with JBoss in it as the JB are treated as one
     */
    @Test
    public void testHyphenateJBossWord() {
        String hyphenated = StringUtil.hyphenate("JBossHome");
        assertEquals("jboss-home", hyphenated);
    }

    /**
     * Test the special case of a word with JBoss in it as the JB are treated as one
     */
    @Test
    public void testHyphenateJBossWordAtEnd() {
        String hyphenated = StringUtil.hyphenate("HomeOfJBoss");
        assertEquals("home-of-jboss", hyphenated);
    }

    /**
     * Test only lower casing first word
     */
    @Test
    public void testLowerCaseFirst() {
        String result = StringUtil.join(StringUtil.lowerCaseFirst(StringUtil.camelHumpsIterator("SomeRootConfig")));
        assertEquals("someRootConfig", result);
    }

    /**
     * Test only lower casing first word and removing a suffix word
     */
    @Test
    public void testLowerCaseFirstNoSuffix() {
        String result = join(withoutSuffix(lowerCaseFirst(camelHumpsIterator("SomeRootConfig")), "Config", "Class"));
        assertEquals("someRoot", result);
        result = join(withoutSuffix(lowerCaseFirst(camelHumpsIterator("SomeRootClass")), "Config", "Class"));
        assertEquals("someRoot", result);
    }

    /**
     * Test only lower casing and removing a suffix word
     */
    @Test
    public void testLowerCaseNoSuffix() {
        String result = join(withoutSuffix(lowerCase(camelHumpsIterator("SomeRootConfig")), "config", "class"));
        assertEquals("someroot", result);
        result = join(withoutSuffix(lowerCase(camelHumpsIterator("SomeRootClass")), "config", "class"));
        assertEquals("someroot", result);
    }

    @Test
    public void testChangePrefix() {
        assertEquals("quarkus.new-prefix.configuration-property", StringUtil
                .changePrefix("quarkus.old-prefix.configuration-property", "quarkus.old-prefix.", "quarkus.new-prefix."));
        assertEquals("quarkus.other-prefix.configuration-property", StringUtil
                .changePrefix("quarkus.other-prefix.configuration-property", "quarkus.old-prefix.", "quarkus.new-prefix."));
    }
}
