package io.quarkus.runtime.util;

import java.util.function.BooleanSupplier;

import io.smallrye.common.os.OS;

/**
 * Platform specific annotations,
 * e.g. @TargetClass(className = "sun.java2d.windows.WindowsFlags", onlyWith = IsWindows.class)
 */
public class IsWindows implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return OS.WINDOWS.isCurrent();
    }
}
