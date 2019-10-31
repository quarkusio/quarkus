package io.quarkus.tck.faulttolerance;

import org.eclipse.microprofile.fault.tolerance.tck.TimeoutUninterruptableTest;
import org.testng.annotations.Test;

/**
 * Customized test from MP FT TCK because `testTimeout` fails because Hystrix is unable to interrupt the calling thread, as it
 * didn't create it.
 * https://github.com/eclipse/microprofile-fault-tolerance/issues/408
 */
public class CustomTimeoutUninterruptableTest extends TimeoutUninterruptableTest {

    @Test(enabled = false)
    public void testTimeout() {
    }
}
