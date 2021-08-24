package io.quarkus.deployment.dev.testing;

import static io.quarkus.deployment.dev.testing.MessageFormat.BLUE;
import static io.quarkus.deployment.dev.testing.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;
import static io.quarkus.deployment.dev.testing.MessageFormat.statusFooter;
import static io.quarkus.deployment.dev.testing.MessageFormat.statusHeader;

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
import io.quarkus.deployment.console.AeshConsole;
import io.quarkus.deployment.console.ConsoleCommand;
import io.quarkus.deployment.console.ConsoleStateManager;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.console.StatusLine;
import io.quarkus.dev.spi.DevModeType;

public class TestConsoleHandler implements TestListener {

    private static final Logger log = Logger.getLogger("io.quarkus.test");

    public static final ConsoleCommand TOGGLE_TEST_OUTPUT = new ConsoleCommand('o', "Toggle test output", "Toggle test output",
            1000,
            new ConsoleCommand.HelpState(TestSupport.instance().get()::isDisplayTestOutput),
            () -> TestSupport.instance().get().toggleTestOutput());

    final DevModeType devModeType;

    boolean firstRun = true;
    boolean disabled = true;
    boolean currentlyFailing = false;
    volatile TestController testController;
    private String lastResults;

    private volatile ConsoleStateManager.ConsoleContext consoleContext;
    private volatile StatusLine resultsOutput;
    private volatile StatusLine testsStatusOutput;
    private volatile StatusLine testsCompileOutput;

    public TestConsoleHandler(DevModeType devModeType) {
        this.devModeType = devModeType;
    }

    public void install() {
        QuarkusClassLoader classLoader = (QuarkusClassLoader) getClass().getClassLoader();
        classLoader.addCloseTask(new Runnable() {
            @Override
            public void run() {
                if (resultsOutput != null) {
                    resultsOutput.close();
                }
                if (testsStatusOutput != null) {
                    testsStatusOutput.close();
                }
                if (consoleContext != null) {
                    consoleContext.reset();
                }
            }
        });
    }

    @Override
    public void listenerRegistered(TestController testController) {
        this.testController = testController;
        this.consoleContext = ConsoleStateManager.INSTANCE.createContext("Continuous Testing");
        this.resultsOutput = QuarkusConsole.INSTANCE.registerStatusLine(QuarkusConsole.TEST_RESULTS);
        this.testsStatusOutput = QuarkusConsole.INSTANCE.registerStatusLine(QuarkusConsole.TEST_STATUS);
        this.testsCompileOutput = QuarkusConsole.INSTANCE.registerStatusLine(QuarkusConsole.COMPILE_ERROR);
        setupPausedConsole();
    }

    private void setupPausedConsole() {
        testsStatusOutput.setMessage(BLUE + "Tests paused" + RESET);
        consoleContext.reset(new ConsoleCommand('r', "Resume testing", "to resume testing", 500, null, new Runnable() {
            @Override
            public void run() {
                if (lastResults == null) {
                    testsStatusOutput.setMessage(BLUE + "Starting tests" + RESET);
                } else {
                    testsStatusOutput.setMessage(null);
                }
                setupTestsRunningConsole();
                TestSupport.instance().get().start();
            }
        }));
        addTestOutput();
    }

    private void setupFirstRunConsole() {
        if (lastResults != null) {
            resultsOutput.setMessage(lastResults);
        } else {
            testsStatusOutput.setMessage(BLUE + "Running tests for the first time" + RESET);
        }
        if (firstRun) {
            consoleContext.reset();
            addTestOutput();
        }
    }

    void addTestOutput() {
        if (devModeType != DevModeType.TEST_ONLY) {
            consoleContext.addCommand(TOGGLE_TEST_OUTPUT);
        }
    }

    private void setupTestsRunningConsole() {
        consoleContext.reset(
                new ConsoleCommand('r', "Re-run all tests", "to re-run", 500, null, () -> {
                    testController.runAllTests();
                }), new ConsoleCommand('f', "Re-run failed tests", null, () -> {
                    testController.runFailedTests();
                }),
                new ConsoleCommand('b', "Toggle 'broken only' mode, where only failing tests are run",
                        new ConsoleCommand.HelpState(testController::isBrokenOnlyMode),
                        () -> testController.toggleBrokenOnlyMode()),
                new ConsoleCommand('v', "Print failures from the last test run", null, () -> testController.printFullResults()),
                new ConsoleCommand('p', "Pause tests", null, () -> TestSupport.instance().get().stop()));
        addTestOutput();
    }

    @Override
    public void testsEnabled() {
        disabled = false;
        setupFirstRunConsole();
    }

    @Override
    public void testsDisabled() {
        disabled = true;
        setupPausedConsole();
    }

    @Override
    public void testCompileFailed(String message) {
        testsCompileOutput.setMessage(message);
    }

    @Override
    public void testCompileSucceeded() {
        testsCompileOutput.setMessage(null);
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
                testsStatusOutput.setMessage("Starting test run, " + toRun + " tests to run.");
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
                if (firstRun) {
                    setupTestsRunningConsole();
                }
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
                            GREEN + "All %d " + pluralize("test is", "tests are", results.getPassedCount()) + " passing "
                                    + "(%d skipped), "
                                    + "%d "
                                    + pluralize("test was", "tests were",
                                            results.getCurrentTotalCount() - results.getCurrentSkippedCount())
                                    + " run in %dms."
                                    + end + RESET,
                            results.getPassedCount(),
                            results.getSkippedCount(),
                            results.getCurrentTotalCount() - results.getCurrentSkippedCount(), results.getTotalTime());
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
                            statusFooter(RED + results.getCurrentFailedCount() + " "
                                    + pluralize("TEST", "TESTS", results.getCurrentFailedCount()) + " FAILED"));
                    lastResults = String.format(
                            RED + "%d " + pluralize("test", "tests", results.getCurrentFailedCount()) + " failed"
                                    + RESET + " (" + GREEN + "%d passing" + RESET + ", " + BLUE + "%d skipped"
                                    + RESET + "), " + RED + "%d " + pluralize("test was", "tests were",
                                            results.getCurrentTotalCount() - results.getCurrentSkippedCount())
                                    + " run in %dms." + end + RESET,
                            results.getCurrentFailedCount(), results.getPassedCount(), results.getSkippedCount(),
                            results.getCurrentTotalCount() - results.getCurrentSkippedCount(), results.getTotalTime());
                }
                //this will re-print when using the basic console
                if (!disabled) {
                    resultsOutput.setMessage(lastResults);
                    testsStatusOutput.setMessage(null);
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
                String status = "Running " + (methodCount.get() + 1) + "/" + totalNoTests
                        + (failureCount.get() == 0 ? "."
                                : ". " + failureCount + " " + pluralize("failure", "failures", failureCount) + " so far.")
                        + " Running: "
                        + className + "#" + testIdentifier.getDisplayName();
                if (TestSupport.instance().get().isDisplayTestOutput() &&
                        QuarkusConsole.INSTANCE instanceof AeshConsole) {
                    log.info(status);
                }
                testsStatusOutput.setMessage(status);
            }
        });

    }

    private static String pluralize(String singular, String plural, long number) {
        if (number == 1L) {
            return singular;
        }

        return plural;
    }

    private static String pluralize(String singular, String plural, AtomicLong number) {
        return pluralize(singular, plural, number.get());
    }

}
