package io.quarkus.test.junit.launcher;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

public class GlobalLauncherSetup implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        System.out.println("HOLLY WAGOO SESSSION OPENED " + session.getLauncher());

        Launcher launcher = session.getLauncher();
        // TODO can we avoid the statics?
        // TODO disabled as these break things
        //        session.getLauncher().registerTestExecutionListeners(new QuarkusTestFilter(session.getLauncher()));
        //        // Avoid setup for test discovery by delaying it until tests are about to be executed
        //        session.getLauncher().registerTestExecutionListeners(new TestExecutionListener() {
        //            @Override
        //            public void testPlanExecutionStarted(TestPlan testPlan) {
        //                System.out.println("HOLLY execution started");

        //         }
        //});

        // session.getLauncher().registerLauncherDiscoveryListeners(new QuarkusTestEngine());
        launcher
                .registerLauncherDiscoveryListeners(new LauncherDiscoveryListener() {
                    @Override
                    public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
                        System.out.println("YOYO discovery started " + request);
                        request.LauncherDiscoveryListener.super.launcherDiscoveryStarted(request);
                    }
                });
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        System.out.println("HOLLY session closed " + session);

    }

}
