package io.quarkus.aesh.deployment;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies that aesh lazy startup works on the Quarkus runtime path:
 * when running a single subcommand of a group command, only the selected
 * path is constructed — the unselected sibling is never instantiated.
 * <p>
 * Each command class has a static construction counter that increments in its
 * constructor. The test runs {@code app run} and asserts that {@code AppCmd}
 * and {@code RunCmd} were constructed but {@code BuildCmd} was not.
 * {@code RunCmd} also injects a CDI bean to prove injection works when the
 * command is constructed lazily during {@code execute()}.
 * <p>
 * The counters are reported via the command's output so they survive the
 * prod-mode process boundary. Note the test is limited to one subcommand:
 * the prod-mode app boots once per test class, so a second subcommand
 * would need its own test class.
 */
public class LazyStartupVerificationTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    AppCmd.class,
                    RunCmd.class,
                    BuildCmd.class,
                    GreetingService.class))
            .setApplicationName("lazy-startup-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("run");

    @Test
    public void testOnlySelectedSubcommandConstructed() {
        String output = config.getStartupConsoleOutput();
        Assertions.assertThat(config.getExitCode()).isZero();

        // AppCmd (top command) should have been constructed during execute()
        Assertions.assertThat(output)
                .as("AppCmd should be constructed")
                .contains("app:constructed=1");

        // RunCmd should have been constructed (counter = 1)
        Assertions.assertThat(output)
                .as("RunCmd should be constructed")
                .contains("run:constructed=1");

        // BuildCmd should NOT have been constructed (counter = 0)
        Assertions.assertThat(output)
                .as("BuildCmd should not be constructed (lazy startup)")
                .contains("build:constructed=0");
    }

    @Test
    public void testCdiInjectionOnLazyPath() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .as("CDI injection should work for lazily constructed commands")
                .contains("Hello Lazy from CDI!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "app", description = "App with lazy subcommands", groupCommands = { RunCmd.class,
            BuildCmd.class })
    public static class AppCmd implements Command<CommandInvocation> {
        static final AtomicInteger constructionCount = new AtomicInteger();

        public AppCmd() {
            constructionCount.incrementAndGet();
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("app:constructed=" + constructionCount.get());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "run", description = "Run subcommand")
    public static class RunCmd implements Command<CommandInvocation> {
        static final AtomicInteger constructionCount = new AtomicInteger();

        @Inject
        GreetingService greetingService;

        public RunCmd() {
            constructionCount.incrementAndGet();
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(greetingService.greet("Lazy"));
            // The top command is constructed during execute() but its own
            // execute() is not invoked for subcommand runs, so its counter
            // is reported from here.
            invocation.println("app:constructed=" + AppCmd.constructionCount.get());
            invocation.println("run:constructed=" + constructionCount.get());
            invocation.println("build:constructed=" + BuildCmd.constructionCount.get());
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build subcommand")
    public static class BuildCmd implements Command<CommandInvocation> {
        static final AtomicInteger constructionCount = new AtomicInteger();

        public BuildCmd() {
            constructionCount.incrementAndGet();
        }

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("build:constructed=" + constructionCount.get());
            invocation.println("run:constructed=" + RunCmd.constructionCount.get());
            return CommandResult.SUCCESS;
        }
    }
}
