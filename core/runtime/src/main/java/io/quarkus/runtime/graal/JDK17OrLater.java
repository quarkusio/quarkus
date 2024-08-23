package io.quarkus.runtime.graal;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.util.JavaVersionUtil;

public class JDK17OrLater implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return JavaVersionUtil.isJava17OrHigher();
    }
}
