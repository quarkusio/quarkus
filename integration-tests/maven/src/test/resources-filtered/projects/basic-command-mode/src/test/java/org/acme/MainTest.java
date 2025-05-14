package org.acme;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
public class MainTest {

    @Test
    @Launch(value = {"one"}, exitCode = 10)
    public void testMain(LaunchResult result) {
        Assertions.assertTrue(result.getOutput().contains("ARGS: [one]"));
    }
}
