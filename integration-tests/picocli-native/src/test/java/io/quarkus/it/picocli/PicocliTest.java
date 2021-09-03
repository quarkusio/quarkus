package io.quarkus.it.picocli;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class PicocliTest {

    @Test
    @Launch({ "test-command", "-f", "test.txt", "-f", "test2.txt", "-f", "test3.txt", "-s", "ERROR", "-h", "SOCKS=5.5.5.5",
            "-p", "privateValue", "pos1", "pos2" })
    public void testBasicReflection(LaunchResult result) throws UnknownHostException {
        assertThat(result.getOutput())
                .contains("-s", "ERROR")
                .contains("-p:privateValue")
                .contains("-p:privateValue")
                .contains("positional:[pos1, pos2]");
    }

    @Test
    public void testMethodSubCommand(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("with-method-sub-command", "hello", "-n", "World!");
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).isEqualTo("Hello World!");
        result = launcher.launch("with-method-sub-command", "goodBye", "-n", "Test?");
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).isEqualTo("Goodbye Test?");
    }

    @Test
    @Launch({ "command-used-as-parent", "-p", "testValue", "child" })
    public void testParentCommand(LaunchResult result) {
        assertThat(result.getOutput()).isEqualTo("testValue");
    }

    @Test
    @Launch({ "exclusivedemo", "-b", "150" })
    public void testCommandWithArgGroup(LaunchResult result) {
        assertThat(result.getOutput())
                .contains("-a:0")
                .contains("-b:150")
                .contains("-c:0");
    }

    @Test
    @Launch({ "dynamic-proxy" })
    public void testDynamicProxy(LaunchResult result) {
        assertThat(result.getOutput()).isEqualTo("2007-12-03T10:15:30");
    }

    @Test
    @Launch("quarkus")
    public void testDynamicVersionProvider(LaunchResult launchResult) {
        assertThat(launchResult.getOutput()).contains("quarkus version 1.0");
    }

    @Test
    @Launch({ "unmatched", "-x", "-a", "AAA", "More" })
    public void testUnmatched(LaunchResult launchResult) {
        assertThat(launchResult.getOutput())
                .contains("-a:AAA")
                .contains("-b:null")
                .contains("remainder:[More]")
                .contains("unmatched[-x]");
    }

    @Test
    public void testI18s(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("localized-command-one", "--help");
        assertThat(result.getOutput())
                .contains("First in CommandOne");
        result = launcher.launch("localized-command-two", "--help");
        assertThat(result.getOutput())
                .contains("First in CommandTwo");
    }

    @Test
    @Launch({ "completion-reflection", "test" })
    public void testCompletionReflection() {

    }

    @Test
    @Launch("default-value-provider")
    public void testDefaultValueProvider(LaunchResult result) {
        assertThat(result.getOutput()).isEqualTo("default:default-value");
    }
}
