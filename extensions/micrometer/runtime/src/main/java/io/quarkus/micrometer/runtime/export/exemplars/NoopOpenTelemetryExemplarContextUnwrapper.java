package io.quarkus.micrometer.runtime.export.exemplars;

import java.util.function.Function;

import jakarta.enterprise.context.Dependent;

import io.vertx.core.Context;

@Dependent
public class NoopOpenTelemetryExemplarContextUnwrapper implements OpenTelemetryContextUnwrapper {

    @Override
    public <P, R> R executeInContext(Function<P, R> methodReference, P parameter, Context requestContext) {
        return methodReference.apply(parameter);// pass through
    }
}
