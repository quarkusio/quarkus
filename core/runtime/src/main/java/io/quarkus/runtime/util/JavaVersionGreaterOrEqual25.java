package io.quarkus.runtime.util;

import java.util.function.BooleanSupplier;

/**
 * Some operations are JDK version dependent, e.g. Netty unsafe.
 *
 */
public class JavaVersionGreaterOrEqual25 implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        return Runtime.version().feature() >= 25;
    }
}
