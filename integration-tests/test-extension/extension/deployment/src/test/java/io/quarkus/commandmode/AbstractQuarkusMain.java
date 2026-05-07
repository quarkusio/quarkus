package io.quarkus.commandmode;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public abstract class AbstractQuarkusMain implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        return 0;
    }
}
