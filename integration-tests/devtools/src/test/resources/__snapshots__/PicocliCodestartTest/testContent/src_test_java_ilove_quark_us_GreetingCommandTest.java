package ilove.quark.us;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class GreetingCommandTest {

    @Test
    public void testBasicLaunch(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch();
        assertTrue(result.getOutput().contains("Hello picocli, go go commando!"), result.getOutput());
        assertEquals(result.exitCode(), 0);
    }

    @Test
    @Launch({ "Alice" })
    public void testLaunchWithArguments(LaunchResult result) {
        assertTrue(result.getOutput().contains("Hello Alice, go go commando!"), result.getOutput());
    }

}
