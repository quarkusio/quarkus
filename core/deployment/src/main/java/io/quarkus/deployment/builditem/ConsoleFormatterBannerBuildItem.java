package io.quarkus.deployment.builditem;

import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

public final class ConsoleFormatterBannerBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Optional<Supplier<String>>> bannerSupplier;

    public ConsoleFormatterBannerBuildItem(final RuntimeValue<Optional<Supplier<String>>> supplierValue) {
        bannerSupplier = supplierValue;
    }

    public RuntimeValue<Optional<Supplier<String>>> getBannerSupplier() {
        return bannerSupplier;
    }
}
