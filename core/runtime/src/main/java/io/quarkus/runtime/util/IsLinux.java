package io.quarkus.runtime.util;

import java.util.function.BooleanSupplier;

import io.smallrye.common.os.OS;

/**
 * Platform specific annotations,
 * allows one to deliver different substitutions for the same target
 * based on the platform, e.g.:
 *
 * @TargetClass(className = "sun.awt.FontConfiguration", onlyWith = IsLinux.class)
 *                        final class Target_sun_awt_FontConfiguration_Linux {...
 *
 * @TargetClass(className = "sun.awt.FontConfiguration", onlyWith = IsWindows.class)
 *                        final class Target_sun_awt_FontConfiguration_Windows {...
 */
public class IsLinux implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return OS.LINUX.isCurrent();
    }
}
