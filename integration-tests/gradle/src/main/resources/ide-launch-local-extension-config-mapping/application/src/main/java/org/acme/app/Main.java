package org.acme.app;

import io.quarkus.runtime.Quarkus;

public class Main {

    public static void main(String... args) {
        Quarkus.run(args);
        System.exit(Boolean.getBoolean(StartupObserver.STARTED) ? 0 : 1);
    }
}
