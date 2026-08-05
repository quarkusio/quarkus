package io.quarkus.test;

import java.io.PrintWriter;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/**
 * Minimal JUnit launcher for running a single test class in a forked JVM.
 * Used by the reproducibility check in {@link AbstractQuarkusExtensionTest}.
 */
class ReproducibilityTestLauncher {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(args[0]))
                .build();

        var summary = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summary);
        launcher.execute(request);

        var result = summary.getSummary();
        if (result.getTotalFailureCount() > 0) {
            result.printFailuresTo(new PrintWriter(System.err, true));
            System.exit(1);
        }
    }
}
