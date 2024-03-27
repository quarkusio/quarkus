package io.quarkus.test.junit.launcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

public class QuarkusTestFilter implements PostDiscoveryFilter, TestExecutionListener {

    // TODO can we avoid the statics? would config help?

    static private Set<String> quarktestNames = new HashSet<>();
    static boolean firstTime = true;

    private final Launcher launcher;

    public QuarkusTestFilter(Launcher launcher) {
        this.launcher = launcher;
    }

    public QuarkusTestFilter() {
        this.launcher = null; // TODO UGH
    }

    @Override
    public FilterResult apply(TestDescriptor testDescriptor) {
        System.out.println("HOLLY applying filters to discovery!" + testDescriptor + testDescriptor.isContainer());
        // Only filter out from the built-in JUnit engines
        if (!QuarkusTestEngine.UNIQUE_ID.equals(testDescriptor.getUniqueId().getEngineId().get())) {
            System.out.println("HOLLY global-filtering " + testDescriptor);
            System.out.println("HOLLY parent is " + testDescriptor.getParent());
            System.out.println("HOLLY parent is " + Arrays.toString(testDescriptor.getTags().toArray()));
            //    allDiscoveredIds.add(testDescriptor.);
            if (testDescriptor.getTags().stream()
                    .anyMatch(tag -> "io.quarkus.test.junit.QuarkusTest".equals(tag.getName()))) {
                System.out.println("excitement! filtering out " + testDescriptor + testDescriptor.getType() + " is test "
                        + testDescriptor.isTest());
                quarktestNames.add(testDescriptor.getDisplayName());
                System.out.println("HOLLY registered " + testDescriptor.getDisplayName());
                // TODO how do we loop round for the test classes but not the methods? is itContainer? or isTest?
                // TODO but why did we want to do that?
                return FilterResult.excluded("QuarkusTest");
                // TODO divide these into batches and profiles and also check the new resource thing
            } else {
                return FilterResult.included(null);
            }
        } else {
            return FilterResult.included(null);
        }
    }

    @Override
    public Predicate<TestDescriptor> toPredicate() {
        return PostDiscoveryFilter.super.toPredicate();
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        System.out.println("OK! we COULD go again");

    }

    @Override
    public void testPlanExecutionFinished(TestPlan oldPlan) {
        System.out.println("OK! we go again");
        firstTime = false;
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        DiscoverySelectors.selectClass(quarktestNames.toArray()[0].toString()))
                //                    .filters(
                //                            includeClassNamePatterns(
                //                                    context.getRequiredTestClass().getName()))
                .build();

        System.out.println("HOLLY request is " + request);
        quarktestNames = new HashSet<>();

        TestPlan testPlan = LauncherFactory.create()
                .discover(request);

        System.out.println("HOLLY made test plan " + testPlan + testPlan.containsTests());

        for (TestIdentifier root : testPlan.getRoots()) {
            System.out.println("Root: " + root.toString());

            for (TestIdentifier test : testPlan.getChildren(root)) {
                System.out.println("Found test: " + test.toString());
            }
        }

        // TODO should we re-use a session here?
        //        try (LauncherSession session = LauncherFactory.openSession()) {
        //            Launcher launcher = session.getLauncher();
        // Register a listener of your choice
        // Execute test plan
        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            launcher.execute(testPlan);
            // Alternatively, execute the request directly
            launcher.execute(request);
        }

    }
}
