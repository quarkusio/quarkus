package io.quarkus.deployment.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.util.HashUtil;

public class HashUtilTest {

    @Test
    public void testSha1() {
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", HashUtil.sha1("test"));
    }
}
