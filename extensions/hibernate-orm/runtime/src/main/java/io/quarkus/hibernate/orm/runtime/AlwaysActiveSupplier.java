package io.quarkus.hibernate.orm.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.ActiveResult;

public final class AlwaysActiveSupplier implements Supplier<ActiveResult> {
    @Override
    public ActiveResult get() {
        return ActiveResult.active();
    }
}
