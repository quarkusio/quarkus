package io.quarkus.deployment.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HashUtilTest {

    @Test
    public void testSha1() {
        assertEquals("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3", HashUtil.sha1("test"));
    }
}
