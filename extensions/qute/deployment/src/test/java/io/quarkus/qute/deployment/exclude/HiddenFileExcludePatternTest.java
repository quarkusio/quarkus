package io.quarkus.qute.deployment.exclude;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class HiddenFileExcludePatternTest {

    @Test
    public void testPattern() {
        Pattern pattern = Pattern.compile("^\\..*|.*\\/\\..*$");
        assertTrue(pattern.matcher(".hidden").matches());
        assertTrue(pattern.matcher("/foo/bar/.hidden").matches());
        assertTrue(pattern.matcher(".foo.txt").matches());
        assertFalse(pattern.matcher("_foo.txt").matches());
        assertFalse(pattern.matcher("/foo/bar/_foo.txt").matches());
    }

}
