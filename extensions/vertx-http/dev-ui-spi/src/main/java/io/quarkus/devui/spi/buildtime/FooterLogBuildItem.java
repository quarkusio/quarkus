package io.quarkus.devui.spi.buildtime;

import java.util.concurrent.Flow;
import java.util.function.Supplier;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;

/**
 * Add a log to the footer of dev ui
 */
public final class FooterLogBuildItem extends AbstractDevUIBuildItem {

    private final String name;
    private final Supplier<Flow.Publisher<String>> publisherSupplier;

    public FooterLogBuildItem(String name, Supplier<Flow.Publisher<String>> publisherSupplier) {
        super(DEV_UI);
        this.name = name;
        this.publisherSupplier = publisherSupplier;
    }

    public String getName() {
        return name;
    }

    public Flow.Publisher<String> getPublisher() {
        return publisherSupplier.get();
    }
}
