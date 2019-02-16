package org.jboss.shamrock.deployment.util;

import org.junit.Assert;
import org.junit.Test;

import static org.jboss.shamrock.deployment.util.StringUtil.camelHumpsIterator;
import static org.jboss.shamrock.deployment.util.StringUtil.join;
import static org.jboss.shamrock.deployment.util.StringUtil.lowerCase;
import static org.jboss.shamrock.deployment.util.StringUtil.lowerCaseFirst;
import static org.jboss.shamrock.deployment.util.StringUtil.withoutSuffix;

/**
 * Tests of the {@linkplain StringUtil} methods
 */
public class StringUtilTestCase {

    /**
     * Test word beginning with mulitiple uppercase letters
     */
    @Test
    public void testHypentateUppercaseBegin() {
        String hyphenated = StringUtil.hyphenate("SBVbt");
        Assert.assertEquals("sb-vbt", hyphenated);
    }
    /**
     * Test word with only uppercase letters
     */
    @Test
    public void testHypentateAllUppercase() {
        String hyphenated = StringUtil.hyphenate("SHOUT");
        Assert.assertEquals("shout", hyphenated);
    }

    /**
     * Test word with multiple uppercase letters in middle
     */
    @Test
    public void testHypentateUppercaseMiddle() {
        String hyphenated = StringUtil.hyphenate("btSBVsuffix");
        Assert.assertEquals("bt-sb-vsuffix", hyphenated);
    }
    /**
     * Test word with multiple uppercase letters at end
     */
    @Test
    public void testHypentateUppercaseEnd() {
        String hyphenated = StringUtil.hyphenate("btSBV");
        Assert.assertEquals("bt-sbv", hyphenated);
    }

    /**
     * Test the special case of a word with JBoss in it as the JB are treated as one
     */
    @Test
    public void testHypentateJBossWord() {
        String hyphenated = StringUtil.hyphenate("JBossHome");
        Assert.assertEquals("jboss-home", hyphenated);
    }
    /**
     * Test the special case of a word with JBoss in it as the JB are treated as one
     */
    @Test
    public void testHypentateJBossWordAtEnd() {
        String hyphenated = StringUtil.hyphenate("HomeOfJBoss");
        Assert.assertEquals("home-of-jboss", hyphenated);
    }

    /**
     * Test only lower casing first word
     */
    @Test
    public void testLowerCaseFirst() {
        String result = StringUtil.join(StringUtil.lowerCaseFirst(StringUtil.camelHumpsIterator("SomeRootConfig")));
        Assert.assertEquals("someRootConfig", result);
    }
    /**
     * Test only lower casing first word and removing a suffix word
     */
    @Test
    public void testLowerCaseFirstNoSuffix() {
        String result = join(withoutSuffix(lowerCaseFirst(camelHumpsIterator("SomeRootConfig")), "Config", "Class"));
        Assert.assertEquals("someRoot", result);
        result = join(withoutSuffix(lowerCaseFirst(camelHumpsIterator("SomeRootClass")), "Config", "Class"));
        Assert.assertEquals("someRoot", result);
    }
    /**
     * Test only lower casing and removing a suffix word
     */
    @Test
    public void testLowerCaseNoSuffix() {
        String result = join(withoutSuffix(lowerCase(camelHumpsIterator("SomeRootConfig")), "config", "class"));
        Assert.assertEquals("someroot", result);
        result = join(withoutSuffix(lowerCase(camelHumpsIterator("SomeRootClass")), "config", "class"));
        Assert.assertEquals("someroot", result);
    }
}