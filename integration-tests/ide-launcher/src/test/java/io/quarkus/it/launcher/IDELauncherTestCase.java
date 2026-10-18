package io.quarkus.it.launcher;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.dev.appstate.ApplicationStateNotification;
import io.quarkus.it.ide.launcher.Main;
import io.quarkus.runtime.Quarkus;
import io.restassured.RestAssured;

/**
 * Tests doing an IDE style launch.
 *
 * Note that this is not a @QuarkusTest, the launch is done via the IDE launcher components.
 */
public class IDELauncherTestCase {

    @Test
    public void testIdeLauncher() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Main.main();
            }
        }).start();
        try {
            ApplicationStateNotification.waitForApplicationStart();
            RestAssured.get("/hello")
                    .then().body(Matchers.equalTo("hello"));
        } finally {
            Quarkus.blockingExit();
        }

    }

}
