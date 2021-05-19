package io.quarkus.deployment.dev.testing;

import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.DEFAULT_COLOR;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.YELLOW;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.statusFooter;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.statusHeader;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.toggleStatus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.dev.console.InputHandler;
import io.quarkus.dev.console.QuarkusConsole;

public class TestConsoleHandler implements TestListener {

    private static final Logger log = Logger.getLogger("io.quarkus.test");

    public static final String PAUSED_PROMPT = YELLOW + "Tests paused, press [r] to resume" + DEFAULT_COLOR;
    public static final String FIRST_RUN_PROMPT = YELLOW + "Running Tests for the first time" + DEFAULT_COLOR;
    public static final String RUNNING_PROMPT = "Press [r] to re-run, [v] to view full results, [p] to pause, [h] for more options>";
    public static final String ABORTED_PROMPT = "Test run aborted.";

    boolean firstRun = true;
    boolean disabled = true;
    volatile InputHandler.ConsoleStatus promptHandler;
    volatile TestController testController;
    private String lastStatus;

    public void install() {
        QuarkusConsole.INSTANCE.pushInputHandler(inputHandler);
        QuarkusClassLoader classLoader = (QuarkusClassLoader) getClass().getClassLoader();
        classLoader.addCloseTask(new Runnable() {
            @Override
            public void run() {
                QuarkusConsole.INSTANCE.popInputHandler();
            }
        });
    }

    private final InputHandler inputHandler = new InputHandler() {

        @Override
        public void handleInput(int[] keys) {
            if (disabled) {
                for (int i : keys) {
                    if (i == 'r') {
                        promptHandler.setStatus(YELLOW + "Starting tests" + DEFAULT_COLOR);
                        TestSupport.instance().get().start();
                    }
                }
            } else if (!firstRun) {
                //TODO: some of this is a bit yuck, this needs some work
                for (int k : keys) {
                    if (k == 'r') {
                        testController.runAllTests();
                    } else if (k == 'f') {
                        testController.runFailedTests();
                    } else if (k == 'v') {
                        testController.printFullResults();
                    } else if (k == 'i') {
                        testController.toggleInstrumentation();
                    } else if (k == 'o') {
                        testController.toggleTestOutput();
                    } else if (k == 'p') {
                        TestSupport.instance().get().stop();
                    } else if (k == 'h') {
                        printUsage();
                    } else if (k == 'b') {
                        testController.toggleBrokenOnlyMode();
                    } else if (k == 'l') {
                        RuntimeUpdatesProcessor.INSTANCE.toggleLiveReloadEnabled();
                    } else if (k == 's') {
                        try {
                            RuntimeUpdatesProcessor.INSTANCE.doScan(true, true);
                        } catch (IOException e) {
                            log.error("Live reload scan failed", e);
                        }
                    }
                }
            }
        }

        @Override
        public void promptHandler(InputHandler.ConsoleStatus promptHandler) {
            TestConsoleHandler.this.promptHandler = promptHandler;
        }
    };

    @Override
    public void listenerRegistered(TestController testController) {
        this.testController = testController;
        promptHandler.setPrompt(PAUSED_PROMPT);
    }

    public void printUsage() {
        System.out.println("\nThe following commands are available:");
        System.out.println("[" + GREEN + "r" + DEFAULT_COLOR + "] - Re-run all tests");
        System.out.println("[" + GREEN + "f" + DEFAULT_COLOR + "] - Re-run failed tests");
        System.out
                .println("[" + GREEN + "b" + DEFAULT_COLOR + "] - Toggle 'broken only' mode, where only failing tests are run "
                        + toggleStatus(testController.isBrokenOnlyMode()));
        System.out.println("[" + GREEN + "v" + DEFAULT_COLOR + "] - Print failures from the last test run");
        System.out.println("[" + GREEN + "o" + DEFAULT_COLOR + "] - Toggle test output "
                + toggleStatus(testController.isDisplayTestOutput()));
        System.out.println("[" + GREEN + "p" + DEFAULT_COLOR + "] - Pause tests");
        System.out.println("[" + GREEN + "i" + DEFAULT_COLOR + "] - Toggle instrumentation based reload "
                + toggleStatus(testController.isInstrumentationEnabled()));
        System.out.println("[" + GREEN + "l" + DEFAULT_COLOR + "] - Toggle live reload "
                + toggleStatus(testController.isLiveReloadEnabled()));
        System.out.println("[" + GREEN + "s" + DEFAULT_COLOR + "] - Force live reload scan");
        System.out.println("[" + GREEN + "h" + DEFAULT_COLOR + "] - Display this help");
    }

    @Override
    public void testsEnabled() {
        disabled = false;
        if (firstRun) {
            promptHandler.setStatus(null);
            promptHandler.setPrompt(FIRST_RUN_PROMPT);
        } else {
            promptHandler.setPrompt(RUNNING_PROMPT);
            promptHandler.setStatus(lastStatus);
        }
    }

    @Override
    public void testsDisabled() {
        disabled = true;
        promptHandler.setPrompt(PAUSED_PROMPT);
        promptHandler.setStatus(null);
    }

    @Override
    public void testRunStarted(Consumer<TestRunListener> listenerConsumer) {

        AtomicLong totalNoTests = new AtomicLong();
        AtomicLong skipped = new AtomicLong();
        AtomicLong methodCount = new AtomicLong();
        AtomicLong failureCount = new AtomicLong();
        listenerConsumer.accept(new TestRunListener() {
            @Override
            public void runStarted(long toRun) {
                totalNoTests.set(toRun);
                promptHandler.setStatus("Running 0/" + toRun + ".");
            }

            @Override
            public void testComplete(TestResult result) {
                if (result.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failureCount.incrementAndGet();
                } else if (result.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.incrementAndGet();
                }
                methodCount.incrementAndGet();
            }

            @Override
            public void runComplete(TestRunResults results) {
                firstRun = false;
                //TODO: this should not use the logger, it should print a nicer status
                log.info(statusHeader("TEST REPORT #" + results.getId()));
                if (results.getCurrentTotalCount() == 0) {
                    // Probably not a common situation, but it already happened (https://github.com/quarkusio/quarkus/issues/17356)
                    lastStatus = RED + "%d No test was run." + DEFAULT_COLOR;
                    log.warn(statusFooter(RED + "NO TEST WAS RUN"));
                } else if (results.getCurrentAbortedCount() > 0 || results.getCurrentFailedCount() > 0) {
                    for (Map.Entry<String, TestClassResult> classEntry : results.getCurrentFailing().entrySet()) {
                        for (TestResult test : classEntry.getValue().getFailing()) {
                            if (test.runId == results.getId()) {
                                log.error(
                                        "Test " + test.getDisplayName() + " failed \n",
                                        test.getTestExecutionResult().getThrowable().get());
                            }

                        }
                        for (TestResult test : classEntry.getValue().getAborted()) {
                            if (test.runId == results.getId()) {
                                log.error("Test " + test.getDisplayName() + " aborted \n" + RED
                                        + test.getTestExecutionResult().getThrowable().get().getMessage());
                            }
                        }
                    }
                    for (TestClassResult classResult : results.getAborted()) {
                        for (TestResult test : classResult.getAborted()) {
                            if (test.runId == results.getId()) {
                                log.error("Test " + test.getDisplayName() + " aborted \n" + RED
                                        + test.getTestExecutionResult().getThrowable().get().getMessage());
                            }
                        }
                    }
                    if (results.getCurrentAbortedCount() > 0) {
                        log.error(statusFooter(RED + results.getCurrentAbortedCount() + " TESTS ABORTED"));
                    }
                    if (results.getCurrentFailedCount() == 0) {
                        lastStatus = String.format(
                                RED + "%d tests aborted (%d passing), %d tests were run in %dms." + DEFAULT_COLOR,
                                results.getCurrentAbortedCount(), results.getCurrentFailedCount(), results.getPassedCount(),
                                results.getCurrentTotalCount(), results.getTotalTime());
                    } else {
                        log.error(statusFooter(RED + results.getCurrentFailedCount() + " TESTS FAILED"));
                        lastStatus = String.format(
                                RED + "%d tests failed (%d passing, %d aborted), %d tests were run in %dms." + DEFAULT_COLOR,
                                results.getCurrentFailedCount(), results.getPassedCount(), results.getCurrentAbortedCount(),
                                results.getCurrentTotalCount(), results.getTotalTime());
                    }
                } else {
                    lastStatus = String.format(
                            GREEN + "All %d tests are passing, %d tests were run in %dms." + DEFAULT_COLOR,
                            results.getPassedCount(),
                            results.getCurrentTotalCount(), results.getTotalTime());
                    log.info(statusFooter(GREEN + results.getCurrentPassedCount() + " TESTS PASSED"));
                }
                //this will re-print when using the basic console
                promptHandler.setPrompt(RUNNING_PROMPT);
                promptHandler.setStatus(lastStatus);
            }

            @Override
            public void noTests() {
                firstRun = false;
                lastStatus = "No tests to run";
                promptHandler.setStatus(lastStatus);
                promptHandler.setPrompt(RUNNING_PROMPT);
            }

            @Override
            public void runAborted() {
                promptHandler.setStatus(ABORTED_PROMPT);
                promptHandler.setPrompt(RUNNING_PROMPT);
                firstRun = false;
            }

            @Override
            public void testStarted(TestIdentifier testIdentifier, String className) {
                promptHandler.setStatus("Running " + methodCount.get() + "/" + totalNoTests
                        + (failureCount.get() == 0 ? "."
                                : ". " + failureCount + " failures so far.")
                        + " Running: "
                        + className + "#" + testIdentifier.getDisplayName());
            }
        });

    }

    static class MessageFormat {

        public static final String RED = "\u001B[91m";
        public static final String GREEN = "\u001b[32m";
        public static final String YELLOW = "\u001b[33m";
        public static final String DEFAULT_COLOR = "\u001b[0m";

        private MessageFormat() {
        }

        public static String statusHeader(String header) {
            return "==================== " + header + DEFAULT_COLOR + " ====================";
        }

        public static String statusFooter(String footer) {
            return ">>>>>>>>>>>>>>>>>>>> " + footer + DEFAULT_COLOR + " <<<<<<<<<<<<<<<<<<<<";
        }

        public static String toggleStatus(boolean enabled) {
            return "(" + (enabled ? GREEN + "enabled" + DEFAULT_COLOR + "" : RED + "disabled") + DEFAULT_COLOR + ")";
        }

    }

}
