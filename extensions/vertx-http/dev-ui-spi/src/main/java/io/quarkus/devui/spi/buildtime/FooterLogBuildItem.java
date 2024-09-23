package io.quarkus.devui.spi.buildtime;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Supplier;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Add a log to the footer of dev ui
 */
public final class FooterLogBuildItem extends AbstractDevUIBuildItem {

    private final String name;
    private final Supplier<Flow.Publisher<String>> publisherSupplier;
    private final RuntimeValue<SubmissionPublisher<String>> runtimePublisher;

    public FooterLogBuildItem(String name, Supplier<Flow.Publisher<String>> publisherSupplier) {
        super(DEV_UI);
        this.name = name;
        this.publisherSupplier = publisherSupplier;
        this.runtimePublisher = null;
    }

    public FooterLogBuildItem(String name, RuntimeValue<SubmissionPublisher<String>> runtimePublisher) {
        super(DEV_UI);
        this.name = name;
        this.runtimePublisher = runtimePublisher;
        this.publisherSupplier = null;
    }

    public boolean hasRuntimePublisher() {
        return runtimePublisher != null;
    }

    public String getName() {
        return name;
    }

    public Flow.Publisher<String> getPublisher() {
        return publisherSupplier.get();
    }

    public RuntimeValue<SubmissionPublisher<String>> getRuntimePublisher() {
        return runtimePublisher;
    }
}
