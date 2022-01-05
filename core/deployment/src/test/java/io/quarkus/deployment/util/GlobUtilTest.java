package io.quarkus.deployment.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GlobUtilTest {

    @Test
    void simple() {
        assertMatch("abc", Arrays.asList("abc"), Arrays.asList("abx", "ab", "abcd", "abc/de"));
    }

    @Test
    void star() {
        assertMatch("*", Arrays.asList("abc", "abx", "ab", "abcd"), Arrays.asList("ab/c"));
        assertMatch("*c", Arrays.asList("abc", "c"), Arrays.asList("abx", "ab", "abcd", "abc/de", "ab/c"));
        assertMatch("a*", Arrays.asList("abc", "a"), Arrays.asList("bx", "b", "bcd", "abc/de"));
        assertMatch("a*/b", Arrays.asList("a/b", "axy/b"), Arrays.asList("a", "b", "a/bc", "bc/b"));
        assertMatch("a*b", Arrays.asList("ab", "axb", "axyb"), Arrays.asList("a/b", "abc", "1ab"));
        assertMatch("a*b*/c", Arrays.asList("axbx/c", "axbx/c"), Arrays.asList("axbx/xxx/c", "axbx/cc"));
    }

    @Test
    void doubleStar() {
        assertMatch("**", Arrays.asList("abc", "abx", "ab", "abcd", "a/b", "/"), Arrays.asList());
        assertMatch("**c", Arrays.asList("abc", "c", "ab/c", "a/b/c"), Arrays.asList("abx", "ab", "abcd", "abc/de"));
        assertMatch("a**", Arrays.asList("abc", "a", "a/bc", "a/b/c"), Arrays.asList("bx", "b", "bcd", "bc/de"));
        assertMatch("a**/b", Arrays.asList("a/b", "axy/b", "a/x/b"), Arrays.asList("a", "b", "a/bc", "bc/b"));
        assertMatch("a**b", Arrays.asList("ab", "axb", "axyb", "a/b", "a/x/b"), Arrays.asList("abc", "1ab"));
        assertMatch("a**b**/c", Arrays.asList("axbx/c", "axbx/c", "a/x/xbx/c", "axbx/xxx/c"), Arrays.asList("axbx/cc"));
    }

    @Test
    void questionMark() {
        assertMatch("a?b", Arrays.asList("axb"), Arrays.asList("ab", "a/b", "abc"));
        assertMatch("a??b", Arrays.asList("axyb"), Arrays.asList("ab", "a/xb"));
    }

    @Test
    void charClass() {
        assertMatch("ab[c]", Arrays.asList("abc"), Arrays.asList("abx", "ab", "abcd", "ab/"));
        assertMatch("ab[c-e]", Arrays.asList("abc", "abd", "abe"), Arrays.asList("abx", "ab", "abcd", "ab/"));
        assertMatch("ab[cde]", Arrays.asList("abc", "abd", "abe"), Arrays.asList("abx", "ab", "abcd", "ab/"));
        assertMatch("ab[!c]", Arrays.asList("abx"), Arrays.asList("abc", "ab", "abcd", "ab/"));
        assertMatch("[-]", Arrays.asList("-"), Arrays.asList("/", "a", "c"));
        assertMatch("[-b]", Arrays.asList("-", "b"), Arrays.asList("/", "a", "c"));
        assertMatch("[b-]", Arrays.asList("-", "b"), Arrays.asList("/", "a", "c"));
    }

    @Test
    void escapeCharClass() {
        assertMatch("[b\\-d]", Arrays.asList("-", "b", "d"), Arrays.asList("/", "\\", "a", "e"));
        assertMatch("[b\\-]", Arrays.asList("-", "b"), Arrays.asList("/", "\\", "a", "c"));
        assertMatch("[\\-b]", Arrays.asList("-", "b"), Arrays.asList("/", "\\", "a", "c"));
        assertMatch("[#-\\-]", Arrays.asList("-", ",", "+"), Arrays.asList("/", "\\", "!", "a"));
        assertMatch("[+-\\-]", Arrays.asList("-", ",", "+"), Arrays.asList("/", "\\", "*", "a"));

        assertMatch("[a\\]b]", Arrays.asList("a", "]", "b"), Arrays.asList("/", "\\"));
        assertMatch("[a\\[b]", Arrays.asList("a", "[", "b"), Arrays.asList("/", "\\"));
        assertMatch("[a&b]", Arrays.asList("a", "&", "b"), Arrays.asList("/", "\\"));
        assertMatch("[a&&b]", Arrays.asList("a", "&", "b"), Arrays.asList("/", "\\"));

        assertMatch("[\\^ab]", Arrays.asList("^", "a", "b"), Arrays.asList("/", "\\"));
        assertMatch("[a\\^b]", Arrays.asList("^", "a", "b"), Arrays.asList("/", "\\"));
        assertMatch("[ab\\^]", Arrays.asList("^", "a", "b"), Arrays.asList("/", "\\"));
    }

    @Test
    void escape() {
        assertMatch("a\\*\\*b", Arrays.asList("a**b"), Arrays.asList("a\\*\\*b", "ab", "axb"));
        assertMatch("a\\*b", Arrays.asList("a*b"), Arrays.asList("a\\*b", "ab", "axb"));
        assertMatch("a\\?b", Arrays.asList("a?b"), Arrays.asList("a\\?b", "ab", "axb"));
    }

    @Test
    void invalid() {
        try {
            assertMatch("a[", Collections.emptyList(), Collections.emptyList());
            Assertions.fail("Expected " + PatternSyntaxException.class.getSimpleName());
        } catch (IllegalStateException expected) {
        }

        try {
            assertMatch("[]a]", Collections.emptyList(), Collections.emptyList());
            Assertions.fail("Expected " + PatternSyntaxException.class.getSimpleName());
        } catch (PatternSyntaxException expected) {
        }
        try {
            assertMatch("\\", Collections.emptyList(), Collections.emptyList());
        } catch (IllegalStateException expected) {
        }
        try {
            assertMatch("[a-b-c]", Collections.emptyList(), Collections.emptyList());
        } catch (IllegalStateException expected) {
        }
        try {
            assertMatch("[", Collections.emptyList(), Collections.emptyList());
        } catch (IllegalStateException expected) {
        }
        try {
            assertMatch("[^", Collections.emptyList(), Collections.emptyList());
        } catch (IllegalStateException expected) {
        }
        try {
            assertMatch("[^bc", Collections.emptyList(), Collections.emptyList());
        } catch (IllegalStateException expected) {
        }
        try {
            assertMatch("a[", Collections.emptyList(), Collections.emptyList());
        } catch (IllegalStateException expected) {
        }

    }

    @Test
    void alternation() {
        assertMatch("file.{txt,adoc}", Arrays.asList("file.txt", "file.adoc"),
                Arrays.asList("file.java", "file.txtx", "file./txt"));
    }

    @Test
    void embeddedAlternation() {
        assertMatch("file.{tx[ab]}", Arrays.asList("file.txa", "file.txb"), Arrays.asList("file.txt", "file.tx/"));
        assertMatch("file.{tx{yz,ab}}", Arrays.asList("file.txyz", "file.txab"), Arrays.asList("file.tx", "file.txbc"));
    }

    static void assertMatch(String glob, List<String> matching, List<String> notMatching) {
        final String re = GlobUtil.toRegexPattern(glob);
        final Pattern pattern = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
        for (String path : matching) {
            final boolean actual = pattern.matcher(path).matches();
            Assertions.assertTrue(actual, String.format("Glob %s should match path %s", glob, path));
        }
        for (String path : notMatching) {
            final boolean actual = pattern.matcher(path).matches();
            Assertions.assertFalse(actual, String.format("Glob %s should not match path %s", glob, path));
        }
    }

}
