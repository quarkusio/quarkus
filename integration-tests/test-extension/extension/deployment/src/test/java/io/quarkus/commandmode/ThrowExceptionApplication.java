package io.quarkus.commandmode;

import io.quarkus.runtime.QuarkusApplication;

public class ThrowExceptionApplication implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        throw new RuntimeException("Exception thrown from application");
    }
}
