package io.quarkus.arc.processor.bcextensions;

import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;

class InvokerInfoImpl implements InvokerInfo {
    final io.quarkus.arc.processor.InvokerInfo arcInvokerInfo;

    InvokerInfoImpl(io.quarkus.arc.processor.InvokerInfo arcInvokerInfo) {
        this.arcInvokerInfo = arcInvokerInfo;
    }
}
