package io.quarkus.signals.it.cmd;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class SignalsCommandModeTest {

    @Test
    @Launch(value = {}, exitCode = 0)
    public void testSignals(LaunchResult result) {
        // Exit code 0 means all assertions in SignalsApp passed
    }
}
