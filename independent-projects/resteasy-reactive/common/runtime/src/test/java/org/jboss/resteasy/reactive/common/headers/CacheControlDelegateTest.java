package org.jboss.resteasy.reactive.common.headers;

import java.util.List;

import jakarta.ws.rs.core.CacheControl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CacheControlDelegateTest {

    private static CacheControl parse(String value) {
        return CacheControlDelegate.INSTANCE.fromString(value);
    }

    @Test
    public void quotedPrivateFieldListIsSplitAndNotMistakenForExtensions() {
        CacheControl cc = parse("private=\"Set-Cookie, Authorization\", max-age=60");

        Assertions.assertTrue(cc.isPrivate());
        Assertions.assertEquals(List.of("Set-Cookie", "Authorization"), cc.getPrivateFields());
        Assertions.assertEquals(60, cc.getMaxAge());
        // The comma inside the quoted field list must not leak out as a bogus cache extension.
        Assertions.assertTrue(cc.getCacheExtension().isEmpty(), cc.getCacheExtension().toString());
    }

    @Test
    public void quotedNoCacheFieldListIsSplit() {
        CacheControl cc = parse("no-cache=\"Set-Cookie, WWW-Authenticate\"");

        Assertions.assertTrue(cc.isNoCache());
        Assertions.assertEquals(List.of("Set-Cookie", "WWW-Authenticate"), cc.getNoCacheFields());
    }

    @Test
    public void extensionValueContainingEqualsIsPreserved() {
        CacheControl cc = parse("ext=a=b");

        Assertions.assertEquals("a=b", cc.getCacheExtension().get("ext"));
    }

    @Test
    public void commonDirectivesStillParse() {
        CacheControl cc = parse("no-store, no-transform, must-revalidate, max-age=0");

        Assertions.assertTrue(cc.isNoStore());
        Assertions.assertTrue(cc.isNoTransform());
        Assertions.assertTrue(cc.isMustRevalidate());
        Assertions.assertEquals(0, cc.getMaxAge());
        Assertions.assertTrue(cc.getCacheExtension().isEmpty());
    }

    @Test
    public void singlePrivateFieldStillParses() {
        CacheControl cc = parse("private=\"Set-Cookie\"");

        Assertions.assertTrue(cc.isPrivate());
        Assertions.assertEquals(List.of("Set-Cookie"), cc.getPrivateFields());
    }
}
