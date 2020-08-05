package io.quarkus.qrs.runtime.core;

import io.quarkus.qrs.runtime.core.serialization.EntityWriter;
import io.quarkus.qrs.runtime.handlers.RestHandler;

public class QrsDeployment {
    private final ExceptionMapping exceptionMapping;
    private final Serialisers serialisers;
    private final RestHandler[] abortHandlerChain;
    private final EntityWriter dynamicEntityWriter;

    public QrsDeployment(ExceptionMapping exceptionMapping, Serialisers serialisers, RestHandler[] abortHandlerChain,
            EntityWriter dynamicEntityWriter) {
        this.exceptionMapping = exceptionMapping;
        this.serialisers = serialisers;
        this.abortHandlerChain = abortHandlerChain;
        this.dynamicEntityWriter = dynamicEntityWriter;
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

    public EntityWriter getDynamicEntityWriter() {
        return dynamicEntityWriter;
    }
}
