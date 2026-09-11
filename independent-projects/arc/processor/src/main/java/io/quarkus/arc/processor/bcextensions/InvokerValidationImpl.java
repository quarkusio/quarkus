package io.quarkus.arc.processor.bcextensions;

import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.inject.build.compatible.spi.InvokerValidation;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AsyncHandlerInfo;

class InvokerValidationImpl implements InvokerValidation {
    private final SharedErrors errors;
    private final List<AsyncHandlerInfo> asyncHandlers;

    InvokerValidationImpl(SharedErrors errors, List<AsyncHandlerInfo> asyncHandlers) {
        this.errors = errors;
        this.asyncHandlers = asyncHandlers;
    }

    @Override
    public void ensureAsyncHandlerExists(Class<?> asyncType, Supplier<String> message) {
        boolean exists = false;
        for (AsyncHandlerInfo asyncHandler : asyncHandlers) {
            if (asyncHandler.asyncType().equals(DotName.createSimple(asyncType))) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            String msg = message.get();
            errors.add(new DeploymentException("Invoker for type " + asyncType.getName() + " does not exist"
                    + (msg != null ? ": " + msg : "")));
        }
    }
}
