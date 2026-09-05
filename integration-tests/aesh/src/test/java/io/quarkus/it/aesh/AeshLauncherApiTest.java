package io.quarkus.it.aesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.aesh.command.CommandResult;
import org.junit.jupiter.api.Test;

import io.quarkus.test.aesh.AeshLauncher;
import io.quarkus.test.aesh.ExecuteOptions;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * Tests for the AeshLauncher API improvements:
 * <ul>
 * <li>Auto-launch on first execute()</li>
 * <li>Auto-close after each test method</li>
 * <li>Error capture via {@link AeshLauncher#getLastError()}</li>
 * <li>Quoted arguments via {@link AeshLauncher#execute(String)}</li>
 * <li>Launch result via {@link AeshLauncher#getLaunchResult()}</li>
 * <li>Clean command output via {@link AeshLauncher#getCommandOutput()}</li>
 * <li>Accumulated output via {@link AeshLauncher#getOutput()} / {@link AeshLauncher#resetOutput()}</li>
 * <li>Running state via {@link AeshLauncher#isRunning()} / {@link AeshLauncher#waitForExit(Duration)}</li>
 * </ul>
 */
@QuarkusMainTest
public class AeshLauncherApiTest {

    @Test
    void errorCapturedFromFailingCommand(AeshLauncher launcher) {
        launcher.execute("fail --message=test-error", CommandResult.FAILURE);

        assertThat(launcher.getLastError())
                .as("Last error should be captured from the failing command")
                .isNotNull();
        assertThat(launcher.getLastError().getMessage())
                .contains("test-error");
        assertThat(launcher.getErrorOutput())
                .contains("test-error");
        assertThat(launcher.getLastExitCode()).isEqualTo(1);
    }

    @Test
    void noErrorOnSuccessfulCommand(AeshLauncher launcher) {
        launcher.execute("hello --name=Success");

        assertThat(launcher.getCommandOutput()).isEqualTo("Hello Success!\n");
        assertThat(launcher.getLastError()).isNull();
        assertThat(launcher.getErrorOutput()).isEmpty();
    }

    @Test
    void errorClearedBetweenCommands(AeshLauncher launcher) {
        // First command fails
        launcher.execute("fail --message=first-error", CommandResult.FAILURE);
        assertThat(launcher.getLastError()).isNotNull();
        assertThat(launcher.getLastExitCode()).isEqualTo(1);

        // Second command succeeds — error and exit code should be cleared
        launcher.execute("hello --name=Cleared");
        assertThat(launcher.getLastError()).isNull();
        assertThat(launcher.getLastExitCode()).isZero();
    }

    @Test
    void executeWithQuotedArgs(AeshLauncher launcher) {
        launcher.execute("echo --text 'hello world'");

        assertThat(launcher.getCommandOutput()).isEqualTo("hello world\n");
    }

    @Test
    void isRunningReflectsReplState(AeshLauncher launcher) {
        assertThat(launcher.isRunning()).isFalse();

        launcher.launch();
        assertThat(launcher.isRunning()).isTrue();

        launcher.exit();
        assertThat(launcher.isRunning()).isFalse();
    }

    @Test
    void waitForExitAfterExit(AeshLauncher launcher) {
        launcher.launch();
        assertThat(launcher.isRunning()).isTrue();

        launcher.exit();

        boolean exited = launcher.waitForExit(Duration.ofSeconds(10));
        assertThat(exited).isTrue();
        assertThat(launcher.isRunning()).isFalse();
    }

    @Test
    void launchResultAvailableAfterExit(AeshLauncher launcher) {
        launcher.execute("hello");
        launcher.exit();

        assertThat(launcher.getLaunchResult())
                .as("LaunchResult should be available after exit")
                .isNotNull();
        assertThat(launcher.getLaunchResult().exitCode()).isZero();
    }

    @Test
    void getCommandOutputIsClean(AeshLauncher launcher) {
        launcher.execute("hello --name=Clean");

        // getCommandOutput() should have exactly the command output — no prompt, no echo
        assertThat(launcher.getCommandOutput())
                .isEqualTo("Hello Clean!\n")
                .doesNotContain("[quarkus]$")
                .doesNotContain("hello --name");
    }

    @Test
    void accumulatedOutputAcrossCommands(AeshLauncher launcher) {
        launcher.execute("hello --name=First");
        launcher.execute("hello --name=Second");

        assertThat(launcher.getOutput())
                .isEqualTo("Hello First!\nHello Second!\n");
    }

    @Test
    void resetOutputClearsAccumulator(AeshLauncher launcher) {
        launcher.execute("hello --name=Before");
        assertThat(launcher.getOutput()).isEqualTo("Hello Before!\n");

        launcher.resetOutput();
        assertThat(launcher.getOutput()).isEmpty();

        launcher.execute("hello --name=After");
        assertThat(launcher.getOutput()).isEqualTo("Hello After!\n");
    }

    @Test
    void getCommandOutputOnlyHasLastCommand(AeshLauncher launcher) {
        launcher.execute("hello --name=First");
        launcher.execute("hello --name=Second");

        // getCommandOutput() only has the last command's output
        assertThat(launcher.getCommandOutput())
                .isEqualTo("Hello Second!\n")
                .doesNotContain("First");
    }

    @Test
    void executeDefaultsToSuccessAssertion(AeshLauncher launcher) {
        // execute(String) defaults to expecting SUCCESS — should throw on failure
        assertThatThrownBy(() -> launcher.execute("fail --message=boom"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("exit code 1")
                .hasMessageContaining("expected 0");
    }

    @Test
    void executeWithExpectedFailure(AeshLauncher launcher) {
        // execute(String, CommandResult) allows expecting non-success results
        launcher.execute("fail --message=expected", CommandResult.FAILURE);
        assertThat(launcher.getLastExitCode()).isEqualTo(1);
    }

    @Test
    void getLastExitCodeReflectsResult(AeshLauncher launcher) {
        launcher.execute("hello --name=World");
        assertThat(launcher.getLastExitCode()).isZero();

        launcher.execute("fail", CommandResult.FAILURE);
        assertThat(launcher.getLastExitCode()).isEqualTo(1);
    }

    @Test
    void autoLaunchOnFirstExecute(AeshLauncher launcher) {
        // No explicit launch() call — should auto-launch
        assertThat(launcher.isRunning()).isFalse();

        launcher.execute("hello --name=AutoLaunch");

        assertThat(launcher.isRunning()).isTrue();
        assertThat(launcher.getCommandOutput()).isEqualTo("Hello AutoLaunch!\n");
    }

    @Test
    void executeWithOptions(AeshLauncher launcher) {
        launcher.execute("fail --message=opts",
                ExecuteOptions.expecting(CommandResult.FAILURE));
        assertThat(launcher.getLastExitCode()).isEqualTo(1);
    }

    @Test
    void executeWithOptionsTimeout(AeshLauncher launcher) {
        launcher.execute("hello --name=Timeout",
                ExecuteOptions.defaults().timeout(Duration.ofSeconds(10)));
        assertThat(launcher.getCommandOutput()).isEqualTo("Hello Timeout!\n");
    }

    @Test
    void interactiveCommandWithPreCannedInput(AeshLauncher launcher) {
        launcher.execute("confirm --action=deploy",
                ExecuteOptions.defaults().input("y"));
        assertThat(launcher.getCommandOutput())
                .contains("Are you sure you want to deploy?")
                .contains("Confirmed: deploy");
    }

    @Test
    void interactiveCommandDeclined(AeshLauncher launcher) {
        launcher.execute("confirm --action=delete",
                ExecuteOptions.expecting(CommandResult.FAILURE).input("n"));
        assertThat(launcher.getCommandOutput())
                .contains("Cancelled: delete");
    }
}
