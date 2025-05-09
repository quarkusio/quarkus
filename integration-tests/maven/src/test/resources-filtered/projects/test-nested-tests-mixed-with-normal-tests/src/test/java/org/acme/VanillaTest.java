package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class VanillaTest {

    @Test
    public void test() {
        assertEquals(new HelloResource().hello(), "Hello from Quarkus REST via config");
    }

    @Test
    public void testTCCL() {
        // This test is looking at internals, not externals, but I think it's a fair enough expectation
        assertEquals(ClassLoader.getSystemClassLoader(), Thread.currentThread().getContextClassLoader());
    }
}
