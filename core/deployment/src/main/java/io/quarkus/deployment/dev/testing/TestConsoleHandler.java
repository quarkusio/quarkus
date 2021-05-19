package io.quarkus.deployment.dev.testing;

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

    public static final String PAUSED_PROMPT = "\u001b[33mTests paused, press [r] to resume\u001b[0m";
    public static final String FIRST_RUN_PROMPT = "\u001b[33mRunning Tests for the first time\u001b[0m";
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
                        promptHandler.setStatus("\u001B[33mStarting tests\u001b[0m");
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
        System.out.println("[\u001b[32mr\u001b[0m] - Re-run all tests");
        System.out.println("[\u001b[32mf\u001b[0m] - Re-run failed tests");
        System.out.println("[\u001b[32mb\u001b[0m] - Toggle 'broken only' mode, where only failing tests are run ("
                + (testController.isBrokenOnlyMode() ? "\u001b[32menabled\u001b[0m" : "\u001B[91mdisabled\u001b[0m") + ")");
        System.out.println("[\u001b[32mv\u001b[0m] - Print failures from the last test run");
        System.out.println("[\u001b[32mo\u001b[0m] - Toggle test output ("
                + (testController.isDisplayTestOutput() ? "\u001b[32menabled\u001b[0m" : "\u001B[91mdisabled\u001b[0m") + ")");
        System.out.println("[\u001b[32mp\u001b[0m] - Pause tests");
        System.out.println("[\u001b[32mi\u001b[0m] - Toggle instrumentation based reload ("
                + (testController.isInstrumentationEnabled() ? "\u001b[32menabled\u001b[0m" : "\u001B[91mdisabled\u001b[0m")
                + ")");
        System.out.println("[\u001b[32ml\u001b[0m] - Toggle live reload ("
                + (testController.isLiveReloadEnabled() ? "\u001b[32menabled\u001b[0m" : "\u001B[91mdisabled\u001b[0m") + ")");
        System.out.println("[\u001b[32ms\u001b[0m] - Force live reload scan");
        System.out.println("[\u001b[32mh\u001b[0m] - Display this help");

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
                if (results.getCurrentFailing().isEmpty()) {
                    lastStatus = String.format(
                            "\u001B[32mAll %d tests are passing (%d skipped), %d tests were run in %dms.\u001b[0m",
                            results.getPassedCount(),
                            results.getSkippedCount(),
                            results.getCurrentTotalCount(), results.getTotalTime());
                    log.info(
                            "====================\u001B[32m TEST REPORT #" + results.getId()
                                    + "\u001b[0m ====================");
                    log.info(
                            ">>>>>>>>>>>>>>>>>>>>\u001B[32m " + results.getCurrentPassedCount()
                                    + " TESTS PASSED\u001b[0m <<<<<<<<<<<<<<<<<<<<");
                } else {
                    //TODO: this should not use the logger, it should print a nicer status
                    log.error(
                            "====================\u001B[91m TEST REPORT #" + results.getId()
                                    + "\u001b[0m ====================");
                    for (Map.Entry<String, TestClassResult> classEntry : results.getCurrentFailing().entrySet()) {
                        for (TestResult test : classEntry.getValue().getFailing()) {
                            log.error(
                                    "Test " + test.getDisplayName() + " failed \n",
                                    test.getTestExecutionResult().getThrowable().get());
                        }
                    }
                    log.error(
                            ">>>>>>>>>>>>>>>>>>>>\u001B[91m " + results.getCurrentFailedCount()
                                    + " TESTS FAILED\u001b[0m <<<<<<<<<<<<<<<<<<<<");
                    lastStatus = String.format(
                            "\u001B[91m%d tests failed (%d passing, %d skipped), %d tests were run in %dms.\u001b[0m",
                            results.getCurrentFailedCount(), results.getPassedCount(), results.getSkippedCount(),
                            results.getCurrentTotalCount(), results.getTotalTime());
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

}
