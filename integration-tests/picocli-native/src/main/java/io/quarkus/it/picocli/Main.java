package io.quarkus.it.picocli;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        // just wait for test execution to finish.
        Quarkus.waitForExit();
        return 0;
    }
}
