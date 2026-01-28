package io.quarkus.it.aesh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class AeshTest {

    @Test
    @Launch({ "hello", "--name=Native" })
    public void testHelloCommand(LaunchResult result) {
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Hello Native!");
    }

    @Test
    public void testHelloDefaultName(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("hello");
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Hello World!");
    }

    @Test
    @Launch({ "run", "build" })
    public void testRunSubcommand(LaunchResult result) {
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Running task: build");
    }

    @Test
    @Launch("version")
    public void testVersionSubcommand(LaunchResult result) {
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Version: 1.0.0");
    }

    @Test
    public void testTopCommandWithNoSubcommand(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch();
        assertThat(result.getOutput()).contains("Use a subcommand");
    }

    @Test
    @Launch(value = "fail", exitCode = 1)
    public void testFailureExitCode(LaunchResult result) {
        assertThat(result.exitCode()).isEqualTo(1);
    }

    @Test
    @Launch(value = "explode", exitCode = 1)
    public void testExceptionExitCode(LaunchResult result) {
        assertThat(result.exitCode()).isEqualTo(1);
    }

    @Test
    @Launch({ "cdi-greet", "--name=CDI" })
    public void testCdiInjection(LaunchResult result) {
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("Hello CDI from service!");
    }

    @Test
    @Launch({ "list-options", "-i", "a", "-i", "b", "-i", "c" })
    public void testOptionList(LaunchResult result) {
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("items: [a, b, c]");
    }

    @Test
    @Launch({ "multi-args", "one", "two", "three" })
    public void testMultipleArguments(LaunchResult result) {
        assertThat(result.exitCode()).isZero();
        assertThat(result.getOutput()).contains("args: [one, two, three]");
    }
}
