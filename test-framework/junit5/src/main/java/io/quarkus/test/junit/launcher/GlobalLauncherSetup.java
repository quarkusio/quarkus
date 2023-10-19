package io.quarkus.test.junit.launcher;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class GlobalLauncherSetup implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        System.out.println("HOLLY WAGOO SESSSION OPENED " + session.getLauncher());

        // TODO can we avoid the statics?
        session.getLauncher().registerTestExecutionListeners(new QuarkusTestFilter(session.getLauncher()));
        // Avoid setup for test discovery by delaying it until tests are about to be executed
        session.getLauncher().registerTestExecutionListeners(new TestExecutionListener() {
            @Override
            public void testPlanExecutionStarted(TestPlan testPlan) {
                System.out.println("HOLLY execution started");

            }
        });

        session.getLauncher().registerLauncherDiscoveryListeners(new LauncherDiscoveryListener() {
            @Override
            public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
                System.out.println("HOLLY discovery started");

            }

            @Override
            public void engineDiscoveryStarted(UniqueId id) {
                System.out.println("HOLLY ENGINE discovery started" + id);
            }
        });
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        System.out.println("HOLLY session closed " + session);

    }

}
