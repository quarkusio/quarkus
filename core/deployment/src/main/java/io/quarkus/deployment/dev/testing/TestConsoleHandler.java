package io.quarkus.deployment.dev.testing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

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
    }

    private final InputHandler inputHandler = new InputHandler() {

        @Override
        public void handleInput(int[] keys) {
            if (disabled) {
                for (int i : keys) {
                    if (i == 'r') {
                        TestSupport.instance().get().start();
                    }
                }
            } else if (!firstRun) {
                //TODO: some of this is a bit yuck, this needs some work
                for (int k : keys) {
                    if (k == 'r') {
                        testController.runAllTests();
                    }
                    if (k == 'f') {
                        testController.runFailedTests();
                    } else if (k == 'v') {
                        printFullResults();
                    } else if (k == 'i') {
                        RuntimeUpdatesProcessor.INSTANCE.toggleInstrumentation();
                    } else if (k == 'o') {
                        TestSupport.instance().get().setDisplayTestOutput(!TestSupport.instance().get().displayTestOutput);
                        if (TestSupport.instance().get().displayTestOutput) {
                            log.info("Test output enabled");
                        } else {
                            log.info("Test output disabled");
                        }
                    } else if (k == 'p') {
                        TestSupport.instance().get().stop();
                    } else if (k == 'h') {
                        printUsage();
                    } else if (k == 'b') {
                        if (testController.toggleBrokenOnlyMode()) {
                            log.info("Broken only mode enabled");
                        } else {
                            log.info("Broken only mode disabled");
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
        promptHandler.setStatus(PAUSED_PROMPT);
    }

    public void printUsage() {
        System.out.println("r - Re-run all tests");
        System.out.println("f - Re-run failed tests");
        System.out.println("b - Toggle 'broken only' mode, where only failing tests are run");
        System.out.println("v - Print failures from the last test run");
        System.out.println("o - Toggle test output");
        System.out.println("i - Toggle instrumentation based reload");
        System.out.println("d - Disable tests");
        System.out.println("h - Display this help");

    }

    private void printFullResults() {
        if (testController.currentState().getFailingClasses().isEmpty()) {
            log.info("All tests passed, no output to display");
        }
        for (TestClassResult i : testController.currentState().getFailingClasses()) {
            for (TestResult failed : i.getFailing()) {
                log.error(
                        "Test " + failed.getDisplayName() + " failed "
                                + failed.getTestExecutionResult().getStatus()
                                + "\n",
                        failed.getTestExecutionResult().getThrowable().get());
            }
        }

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
                    lastStatus = "\u001B[32mTests all passed, " + methodCount.get() + " tests were run, " + skipped.get()
                            + " were skipped. Tests took " + (results.getTotalTime())
                            + "ms." + "\u001b[0m";
                } else {
                    int failedTestsNum = results.getCurrentFailing().size();
                    boolean hasFailingTests = failedTestsNum > 0;
                    for (Map.Entry<String, TestClassResult> classEntry : results.getCurrentFailing().entrySet()) {
                        for (TestResult test : classEntry.getValue().getFailing()) {
                            log.error(
                                    "Test " + test.getDisplayName() + " failed \n",
                                    test.getTestExecutionResult().getThrowable().get());
                        }
                    }
                    String output = String.format("Test run failed, %d tests were run, ", methodCount.get())
                            + String.format("%s%d failed%s, ",
                                    hasFailingTests ? "\u001B[1m" : "", failedTestsNum,
                                    hasFailingTests ? "\u001B[2m" : "")
                            + String.format("%d were skipped. Tests took %dms", skipped.get(), results.getTotalTime());
                    lastStatus = "\u001B[91m" + output + "\u001b[0m";
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
