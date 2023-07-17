package io.quarkus.commandmode;

import io.quarkus.runtime.QuarkusApplication;

public class HelloWorldNamed implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        System.out.println("Hello Named");
        return 100;
    }
}
