package io.quarkus.it;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {

    public static volatile String[] PARAMS;

    @Override
    public int run(String... args) throws Exception {
        PARAMS = args;
        Quarkus.waitForExit();
        return 0;
    }
}
