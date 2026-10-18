package io.quarkus.info.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class GitUtilTest {

    @Test
    public void testSanitizeRemoteUrl() {
        assertNull(GitUtil.sanitizeRemoteUrl(null));
        assertNull(GitUtil.sanitizeRemoteUrl(""));
        assertNull(GitUtil.sanitizeRemoteUrl("    "));
        assertEquals("github.com:gsmet/quarkusio.github.io.git",
                GitUtil.sanitizeRemoteUrl("git@github.com:gsmet/quarkusio.github.io.git"));
        assertEquals("github.com:gsmet/quarkusio.github.io.git",
                GitUtil.sanitizeRemoteUrl("  git@github.com:gsmet/quarkusio.github.io.git   "));
        assertEquals("https://github.com/gsmet/quarkusio.github.io.git",
                GitUtil.sanitizeRemoteUrl("https://github.com/gsmet/quarkusio.github.io.git"));
        assertEquals("https://github.com/gsmet/quarkusio.github.io.git",
                GitUtil.sanitizeRemoteUrl("https://gsmet:password@github.com/gsmet/quarkusio.github.io.git"));
        assertEquals("http://github.com/gsmet/quarkusio.github.io.git",
                GitUtil.sanitizeRemoteUrl("http://gsmet:password@github.com/gsmet/quarkusio.github.io.git"));
    }
}
