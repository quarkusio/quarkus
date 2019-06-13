package io.quarkus.undertow.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.undertow.Undertow;

public final class UndertowBuildItem extends SimpleBuildItem {

    final RuntimeValue<Undertow> undertow;

    public UndertowBuildItem(RuntimeValue<Undertow> undertow) {
        this.undertow = undertow;
    }

    public RuntimeValue<Undertow> getUndertow() {
        return undertow;
    }
}
