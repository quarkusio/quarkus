package io.quarkus.deployment.builditem;

import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * A {@link SimpleBuildItem} that loads an optional banner provider for the console formatter.
 *
 * <p>
 * Produced during compilation and obtained by the registry configuration to provide a banner at runtime by
 * via
 * {@link RuntimeValue}.
 */
public final class ConsoleFormatterBannerBuildItem extends SimpleBuildItem {
    /**
     * A RuntimeValue that holds an Optional Supplier of String.
     * The Supplier is used to generate the banner text when needed.
     * The Optional allows for the possibility that no banner supplier is provided, in which case the banner will not be
     * printed.
     */
    private final RuntimeValue<Optional<Supplier<String>>> bannerSupplier;

    /**
     * Creates a build item holding the optional banner supplier.
     *
     * @param supplierValue runtime value that may contain the banner supplier
     */
    public ConsoleFormatterBannerBuildItem(final RuntimeValue<Optional<Supplier<String>>> supplierValue) {
        bannerSupplier = supplierValue;
    }

    /**
     * @return runtime value containing the optional banner supplier
     */
    public RuntimeValue<Optional<Supplier<String>>> getBannerSupplier() {
        return bannerSupplier;
    }
}
