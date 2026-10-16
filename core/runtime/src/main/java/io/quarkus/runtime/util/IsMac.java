package io.quarkus.runtime.util;

import java.util.function.BooleanSupplier;

import io.smallrye.common.os.OS;

/**
 * Platform specific annotations,
 * e.g. @TargetClass(className = "sun.awt.FontConfiguration", onlyWith = IsMac.class)
 */
public class IsMac implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return OS.MAC.isCurrent();
    }
}
