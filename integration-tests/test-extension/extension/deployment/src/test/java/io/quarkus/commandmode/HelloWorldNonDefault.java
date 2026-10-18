package io.quarkus.commandmode;

import io.quarkus.runtime.QuarkusApplication;

public class HelloWorldNonDefault implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        System.out.println("Hello Non Default");
        return 20;
    }
}
