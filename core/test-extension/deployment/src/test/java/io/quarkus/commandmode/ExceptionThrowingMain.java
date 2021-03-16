package io.quarkus.commandmode;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ExceptionThrowingMain implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        String arg = args[100];
        return 0;
    }
}
