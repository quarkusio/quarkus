package io.quarkus.arc.impl;

import java.util.function.Supplier;

/**
 * Meant to be used by generated code in order to easily construct a supplier for lazily created values
 */
public abstract class AbstractValueSupplier<T> implements Supplier<T> {
}
