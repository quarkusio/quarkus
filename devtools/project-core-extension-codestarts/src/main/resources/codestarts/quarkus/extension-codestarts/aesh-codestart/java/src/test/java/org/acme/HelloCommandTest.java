package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class HelloCommandTest {

    @Test
    public void testBasicLaunch(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch();
        assertTrue(result.getOutput().contains("Hello aesh, go go commando!"), result.getOutput());
        assertEquals(result.exitCode(), 0);
    }

    @Test
    @Launch({ "--name=Alice" })
    public void testLaunchWithArguments(LaunchResult result) {
        assertTrue(result.getOutput().contains("Hello Alice, go go commando!"), result.getOutput());
    }

}
