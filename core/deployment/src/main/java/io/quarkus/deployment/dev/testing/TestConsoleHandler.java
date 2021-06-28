package io.quarkus.deployment.dev.testing;

import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.BLUE;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.TestConsoleHandler.MessageFormat.RESET;
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
import io.quarkus.deployment.dev.console.AeshConsole;
import io.quarkus.dev.console.InputHandler;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.spi.DevModeType;

public class TestConsoleHandler implements TestListener {

    private static final Logger log = Logger.getLogger("io.quarkus.test");

    public static final String PAUSED_PROMPT = "Tests paused, press [" + BLUE + "r" + RESET + "] to resume, [" + BLUE + "h"
            + RESET + "] for more options>" + RESET;
    public static final String PAUSED_PROMPT_NO_HTTP = "Tests paused, press [" + BLUE + "r" + RESET + "] to resume, [" + BLUE
            + "s" + RESET + "] to restart with changes, [" + BLUE + "h"
            + RESET + "] for more options>" + RESET;
    public static final String FIRST_RUN_PROMPT = BLUE + "Running Tests for the first time" + RESET;
    public static final String RUNNING_PROMPT = "Press [" + BLUE + "r" + RESET + "] to re-run, [" + BLUE
            + "v" + RESET + "] to view full results, [" + BLUE + "p" + RESET + "] to pause, [" + BLUE
            + "h" + RESET + "] for more options>";
    public static final String RUNNING_PROMPT_NO_HTTP = "Press [" + BLUE + "r" + RESET + "] to re-run, [" + BLUE
            + "v" + RESET + "] to view full results, [" + BLUE + "p" + RESET + "] to pause, [" + BLUE + "s" + RESET
            + "] to restart with changes, [" + BLUE
            + "h" + RESET + "] for more options>";

    final DevModeType devModeType;

    boolean firstRun = true;
    boolean disabled = true;
    boolean currentlyFailing = false;
    volatile InputHandler.ConsoleStatus promptHandler;
    volatile TestController testController;
    private String lastResults;

    /**
     * If HTTP is not present we add the 'press s to reload' option to the prompt
     * to make it clear to users they can restart their apps.
     */
    private final boolean hasHttp;

    public TestConsoleHandler(DevModeType devModeType, boolean hasHttp) {
        this.devModeType = devModeType;
        this.hasHttp = hasHttp;
    }

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
            //common commands, work every time
            for (int k : keys) {
                if (k == 'h') {
                    printUsage();
                } else if (k == 'i' && devModeType != DevModeType.TEST_ONLY) {
                    testController.toggleInstrumentation();
                } else if (k == 'l' && devModeType != DevModeType.TEST_ONLY) {
                    RuntimeUpdatesProcessor.INSTANCE.toggleLiveReloadEnabled();
                } else if (k == 's' && devModeType != DevModeType.TEST_ONLY) {
                    try {
                        RuntimeUpdatesProcessor.INSTANCE.doScan(true, true);
                    } catch (IOException e) {
                        log.error("Live reload scan failed", e);
                    }
                } else if (k == 'q') {
                    //we don't call Quarkus.exit() here as that would just result
                    //in a 'press any key to restart' prompt
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    }, "Quarkus exit thread").run();
                } else {
                    if (disabled) {
                        if (k == 'r') {
                            promptHandler.setStatus(BLUE + "Starting tests" + RESET);
                            TestSupport.instance().get().start();
                        }
                    } else if (!firstRun) {
                        //TODO: some of this is a bit yuck, this needs some work
                        if (k == 'r') {
                            testController.runAllTests();
                        } else if (k == 'f') {
                            testController.runFailedTests();
                        } else if (k == 'v') {
                            testController.printFullResults();
                        } else if (k == 'o' && devModeType != DevModeType.TEST_ONLY) {
                            testController.toggleTestOutput();
                        } else if (k == 'p') {
                            TestSupport.instance().get().stop();
                        } else if (k == 'b') {
                            testController.toggleBrokenOnlyMode();
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
        promptHandler.setPrompt(hasHttp ? PAUSED_PROMPT : PAUSED_PROMPT_NO_HTTP);

    }

    public void printUsage() {
        System.out.println(RESET + "\nThe following commands are available:");
        if (disabled) {
            System.out.println(helpOption("r", "Resume testing"));
        } else {
            System.out.println(helpOption("r", "Re-run all tests"));
            System.out.println(helpOption("f", "Re-run failed tests"));
            System.out.println(helpOption("b", "Toggle 'broken only' mode, where only failing tests are run",
                    testController.isBrokenOnlyMode()));
            System.out.println(helpOption("v", "Print failures from the last test run"));
            if (devModeType != DevModeType.TEST_ONLY) {
                System.out.println(helpOption("o", "Toggle test output", testController.isDisplayTestOutput()));
            }
            System.out.println(helpOption("p", "Pause tests"));
        }
        if (devModeType != DevModeType.TEST_ONLY) {
            System.out
                    .println(helpOption("i", "Toggle instrumentation based reload", testController.isInstrumentationEnabled()));
            System.out.println(helpOption("l", "Toggle live reload", testController.isLiveReloadEnabled()));
            System.out.println(helpOption("s", "Force restart with any changes"));
        }
        System.out.println(helpOption("h", "Display this help"));
        System.out.println(helpOption("q", "Quit"));
    }

    @Override
    public void testsEnabled() {
        disabled = false;
        if (firstRun) {
            promptHandler.setStatus(null);
            promptHandler.setResults(null);
            promptHandler.setPrompt(FIRST_RUN_PROMPT);
        } else {
            promptHandler.setPrompt(hasHttp ? RUNNING_PROMPT : RUNNING_PROMPT_NO_HTTP);
            promptHandler.setResults(lastResults);
            promptHandler.setStatus(null);
        }
    }

    @Override
    public void testsDisabled() {
        disabled = true;
        promptHandler.setPrompt(hasHttp ? PAUSED_PROMPT : PAUSED_PROMPT_NO_HTTP);
        promptHandler.setStatus(null);
        promptHandler.setResults(null);
    }

    @Override
    public void testCompileFailed(String message) {
        promptHandler.setCompileError(message);
    }

    @Override
    public void testCompileSucceeded() {
        promptHandler.setCompileError(null);
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
                    lastResults = BLUE + "No tests found" + RESET;
                } else if (results.getFailedCount() == 0 && results.getPassedCount() == 0) {
                    lastResults = String.format(BLUE + "All %d tests were skipped" + RESET, results.getSkippedCount());
                } else if (results.getCurrentFailing().isEmpty()) {
                    if (currentlyFailing) {
                        log.info(GREEN + "All tests are now passing" + RESET);
                    }
                    currentlyFailing = false;
                    lastResults = String.format(
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
                    lastResults = String.format(
                            RED + "%d tests failed" + RESET + " (" + GREEN + "%d passing" + RESET + ", " + BLUE + "%d skipped"
                                    + RESET + ")" + RED + ", %d tests were run in %dms." + end + RESET,
                            results.getCurrentFailedCount(), results.getPassedCount(), results.getSkippedCount(),
                            results.getCurrentTotalCount(), results.getTotalTime());
                }
                //this will re-print when using the basic console
                if (!disabled) {
                    promptHandler.setPrompt(hasHttp ? RUNNING_PROMPT : RUNNING_PROMPT_NO_HTTP);
                    promptHandler.setResults(lastResults);
                    promptHandler.setStatus(null);
                }
            }

            @Override
            public void noTests(TestRunResults results) {
                runComplete(results);
            }

            @Override
            public void runAborted() {
                firstRun = false;
            }

            @Override
            public void testStarted(TestIdentifier testIdentifier, String className) {
                String status = "Running " + methodCount.get() + "/" + totalNoTests
                        + (failureCount.get() == 0 ? "."
                                : ". " + failureCount + " failures so far.")
                        + " Running: "
                        + className + "#" + testIdentifier.getDisplayName();
                if (TestSupport.instance().get().isDisplayTestOutput() &&
                        QuarkusConsole.INSTANCE instanceof AeshConsole) {
                    log.info(status);
                }
                promptHandler.setStatus(status);
            }
        });

    }

    static class MessageFormat {

        public static final String RED = "\u001B[91m";
        public static final String GREEN = "\u001b[32m";
        public static final String BLUE = "\u001b[34m";
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
            return "[" + BLUE + key + RESET + "] - " + description;
        }

        public static String helpOption(String key, String description, boolean enabled) {
            return helpOption(key, description) + toggleStatus(enabled);
        }

    }

}
