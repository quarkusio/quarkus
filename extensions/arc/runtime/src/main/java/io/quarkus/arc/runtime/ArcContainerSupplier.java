package io.quarkus.arc.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

public class ArcContainerSupplier implements Supplier<ArcContainer> {
    @Override
    public ArcContainer get() {
        return Arc.container();
    }
}
