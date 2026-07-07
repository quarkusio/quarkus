package io.quarkus.gradle.application;

import java.io.PrintWriter;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/**
 * Runs one fixture test in a process whose working directory is the Gradle
 * application. This is equivalent to an IDE's finite JUnit launch: the
 * Quarkus JUnit extension obtains the TEST application model from Gradle
 * through its normal {@code AppMakerHelper} path.
 */
public final class FiniteQuarkusTestLauncher {

    static final String SUCCESS_MARKER = "FINITE_QUARKUS_JUNIT_TEST_SUCCEEDED";

    private FiniteQuarkusTestLauncher() {
    }

    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(args[0]))
                .build();
        var summary = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summary);
        launcher.execute(request);

        var result = summary.getSummary();
        if (result.getTestsFoundCount() != 1
                || result.getTestsSucceededCount() != 1
                || result.getTotalFailureCount() != 0) {
            result.printFailuresTo(new PrintWriter(System.err, true));
            System.err.printf("Expected one successful test, but found %d, succeeded %d, and failed %d.%n",
                    result.getTestsFoundCount(), result.getTestsSucceededCount(), result.getTotalFailureCount());
            System.exit(1);
        }
        System.out.println(SUCCESS_MARKER);
    }
}
