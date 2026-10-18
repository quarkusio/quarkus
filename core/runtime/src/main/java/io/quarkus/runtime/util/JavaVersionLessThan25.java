package io.quarkus.runtime.util;

import java.util.function.BooleanSupplier;

/**
 * Some substitutions are JDK version dependent, e.g.
 *
 * @TargetClass(className = "sun.awt.im.CompositionAreaHandler", onlyWith = { IsWindows.class, JavaVersionLessThan25.class })
 */
public class JavaVersionLessThan25 implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return Runtime.version().feature() < 25;
    }
}
