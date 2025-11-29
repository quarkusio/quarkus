package io.quarkus.arc.test.supplement;

import jakarta.enterprise.context.Dependent;

@Dependent
public class SomeDepBeanInExternalLibrary implements SomeInterfaceInExternalLibrary {
    public static boolean pinged;

    @Override
    public String hello() {
        return "DepHello";
    }

    // methods below are intentionally package-private to verify
    // behavior in Quarkus dev mode (multiple classloaders)

    String ping() {
        pinged = true;
        return "pong";
    }
}
