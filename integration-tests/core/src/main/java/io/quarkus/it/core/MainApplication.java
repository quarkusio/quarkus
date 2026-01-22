package io.quarkus.it.core;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class MainApplication {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
