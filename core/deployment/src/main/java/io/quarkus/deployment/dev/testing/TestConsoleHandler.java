package io.quarkus.deployment.dev.testing;

import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.RESET;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.YELLOW;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.helpOption;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.statusFooter;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.statusHeader;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.dev.console.InputHandler;
import io.quarkus.dev.console.QuarkusConsole;

public class TestConsoleHandler implements TestListener {

    private static final Logger log = Logger.getLogger("io.quarkus.test");

    public static final String PAUSED_PROMPT = YELLOW + "Tests paused, press [r] to resume" + RESET;
    public static final String FIRST_RUN_PROMPT = YELLOW + "Running Tests for the first time" + RESET;
    public static final String RUNNING_PROMPT = "Press [r] to re-run, [v] to view full results, [p] to pause, [h] for more options>";
    public static final String ABORTED_PROMPT = "Test run aborted.";

    boolean firstRun = true;
    boolean disabled = true;
    boolean currentlyFailing = false;
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
                        promptHandler.setStatus(YELLOW + "Starting tests" + RESET);
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
        System.out.println(RESET + "\nThe following commands are available:");
        System.out.println(helpOption("r", "Re-run all tests"));
        System.out.println(helpOption("f", "Re-run failed tests"));
        System.out.println(helpOption("b", "Toggle 'broken only' mode, where only failing tests are run",
                testController.isBrokenOnlyMode()));
        System.out.println(helpOption("v", "Print failures from the last test run"));
        System.out.println(helpOption("o", "Toggle test output", testController.isDisplayTestOutput()));
        System.out.println(helpOption("p", "Pause tests"));
        System.out.println(helpOption("i", "Toggle instrumentation based reload", testController.isInstrumentationEnabled()));
        System.out.println(helpOption("l", "Toggle live reload", testController.isLiveReloadEnabled()));
        System.out.println(helpOption("s", "Force live reload scan"));
        System.out.println(helpOption("h", "Display this help"));
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
                SimpleDateFormat df = new SimpleDateFormat("kk:mm:ss");

                String end = " Tests completed at " + df.format(new Date());
                if (results.getTrigger() != null) {
                    ClassScanResult trigger = results.getTrigger();
                    Set<Path> paths = new LinkedHashSet<>();
                    paths.addAll(trigger.getChangedClasses());
                    paths.addAll(trigger.getAddedClasses());
                    paths.addAll(trigger.getDeletedClasses());
                    if (paths.size() == 1) {
                        end = end + " due to changes to " + paths.iterator().next().getFileName() + ".";
                    } else if (paths.size() > 1) {
                        end = end + " due to changes to " + paths.iterator().next().getFileName() + " and " + (paths.size() - 1)
                                + " other files.";
                    } else {
                        //should never happen
                        end = end + ".";
                    }
                } else {
                    end = end + ".";
                }
                if (results.getTotalCount() == 0) {
                    lastStatus = YELLOW + "No tests found" + RESET;
                } else if (results.getFailedCount() == 0 && results.getPassedCount() == 0) {
                    lastStatus = String.format(YELLOW + "All %d tests were skipped" + RESET, results.getSkippedCount());
                } else if (results.getCurrentFailing().isEmpty()) {
                    if (currentlyFailing) {
                        log.info(GREEN + "All tests are now passing" + RESET);
                    }
                    currentlyFailing = false;
                    lastStatus = String.format(
                            GREEN + "All %d tests are passing (%d skipped), %d tests were run in %dms." + end + RESET,
                            results.getPassedCount(),
                            results.getSkippedCount(),
                            results.getCurrentTotalCount() - results.getSkippedCount(), results.getTotalTime());
                } else {
                    currentlyFailing = true;
                    //TODO: this should not use the logger, it should print a nicer status
                    log.error(statusHeader("TEST REPORT #" + results.getId()));
                    for (Map.Entry<String, TestClassResult> classEntry : results.getCurrentFailing().entrySet()) {
                        for (TestResult test : classEntry.getValue().getFailing()) {
                            log.error(
                                    RED + "Test " + test.getDisplayName() + " failed \n" + RESET,
                                    test.getTestExecutionResult().getThrowable().get());
                        }
                    }
                    log.error(
                            statusFooter(RED + results.getCurrentFailedCount() + " TESTS FAILED"));
                    lastStatus = String.format(
                            RED + "%d tests failed" + RESET + " (" + GREEN + "%d passing" + RESET + ", " + YELLOW + "%d skipped"
                                    + RESET + "), %d tests were run in %dms." + end + RESET,
                            results.getCurrentFailedCount(), results.getPassedCount(), results.getSkippedCount(),
                            results.getCurrentTotalCount(), results.getTotalTime());
                }
                //this will re-print when using the basic console
                promptHandler.setPrompt(RUNNING_PROMPT);
                promptHandler.setStatus(lastStatus);
            }

            @Override
            public void noTests(TestRunResults results) {
                runComplete(results);
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
        public static final String RESET = "\u001b[0m";

        private MessageFormat() {
        }

        public static String statusHeader(String header) {
            return RESET + "==================== " + header + RESET + " ====================";
        }

        public static String statusFooter(String footer) {
            return RESET + ">>>>>>>>>>>>>>>>>>>> " + footer + RESET + " <<<<<<<<<<<<<<<<<<<<";
        }

        public static String toggleStatus(boolean enabled) {
            return " (" + (enabled ? GREEN + "enabled" + RESET + "" : RED + "disabled") + RESET + ")";
        }

        public static String helpOption(String key, String description) {
            return "[" + GREEN + key + RESET + "] - " + description;
        }

        public static String helpOption(String key, String description, boolean enabled) {
            return helpOption(key, description) + toggleStatus(enabled);
        }

    }

}
