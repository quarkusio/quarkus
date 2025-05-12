package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import io.quarkus.runtime.LaunchMode;

public class VanillaTest {

    @Test
    public void test() {
        assertEquals(new HelloResource().hello(), "Hello from Quarkus REST via config");
    }

    @DisabledIf("isContinuousTesting")
    @Test
    public void testTCCL() {
        // This test is looking at internals, not externals, but I think it's a fair enough expectation
        // In continuous testing mode the normal tests get loaded with the deployment classloader, so we should not make assertions
        assertEquals(ClassLoader.getSystemClassLoader(), Thread.currentThread().getContextClassLoader());
    }

    public boolean isContinuousTesting() {
        return LaunchMode.current().isDevOrTest();
    }
}
