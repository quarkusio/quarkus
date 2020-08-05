package io.quarkus.qrs.runtime.core;

import io.quarkus.qrs.runtime.handlers.RestHandler;

public class QrsDeployment {
    private final ExceptionMapping exceptionMapping;
    private final Serialisers serialisers;
    private final RestHandler[] abortHandlerChain;

    public QrsDeployment(ExceptionMapping exceptionMapping, Serialisers serialisers, RestHandler[] abortHandlerChain) {
        this.exceptionMapping = exceptionMapping;
        this.serialisers = serialisers;
        this.abortHandlerChain = abortHandlerChain;
    }

    public ExceptionMapping getExceptionMapping() {
        return exceptionMapping;
    }

    public Serialisers getSerialisers() {
        return serialisers;
    }

    public RestHandler[] getAbortHandlerChain() {
        return abortHandlerChain;
    }
}
