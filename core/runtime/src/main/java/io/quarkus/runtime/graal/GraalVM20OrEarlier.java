package io.quarkus.runtime.graal;

import java.util.function.BooleanSupplier;

import org.graalvm.home.HomeFinder;

public class GraalVM20OrEarlier implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        String version = HomeFinder.getInstance().getVersion();
        int dot = version.indexOf('.');
        if (dot < 0) {
            return false;
        }
        try {
            int major = Integer.parseInt(version.substring(0, dot));
            return major < 21;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
