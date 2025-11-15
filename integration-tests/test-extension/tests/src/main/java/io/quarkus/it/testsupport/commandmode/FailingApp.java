package io.quarkus.it.testsupport.commandmode;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/*
 * Because this app co-exists in a module with a QuarkusIntegrationTest, it needs to not be on the default path.
 * Otherwise, this application is executed by the QuarkusIntegrationTest and exits early, causing test failures elsewhere.
 */
@QuarkusMain(name = "failing-application")
public class FailingApp implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        // the point of this is to verify that Quarkus can properly shut down in the face of an Error (not an Exception)
        throw new NoSuchMethodError("dummy");
    }
}
