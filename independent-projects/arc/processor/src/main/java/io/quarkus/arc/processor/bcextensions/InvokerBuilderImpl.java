package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.invoke.InvokerBuilder;

class InvokerBuilderImpl implements InvokerBuilder<InvokerInfo> {
    private final io.quarkus.arc.processor.InvokerBuilder arcInvokerBuilder;

    InvokerBuilderImpl(io.quarkus.arc.processor.InvokerBuilder arcInvokerBuilder) {
        this.arcInvokerBuilder = arcInvokerBuilder;
    }

    @Override
    public InvokerBuilder<InvokerInfo> withInstanceLookup() {
        arcInvokerBuilder.withInstanceLookup();
        return this;
    }

    @Override
    public InvokerBuilder<InvokerInfo> withArgumentLookup(int position) {
        arcInvokerBuilder.withArgumentLookup(position);
        return this;
    }

    @Override
    public InvokerInfo build() {
        return new InvokerInfoImpl(arcInvokerBuilder.build());
    }
}
