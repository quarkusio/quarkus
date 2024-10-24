package io.quarkus.micrometer.runtime.export.exemplars;

import java.util.function.Function;

import jakarta.enterprise.context.Dependent;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

@Dependent
public class OpenTelemetryExemplarContextUnwrapper implements OpenTelemetryContextUnwrapper {

    @Override
    public <P, R> R executeInContext(Function<P, R> methodReference, P parameter, io.vertx.core.Context requestContext) {
        if (requestContext == null) {
            return methodReference.apply(parameter);
        }

        Context newContext = QuarkusContextStorage.getContext(requestContext);

        if (newContext == null) {
            return methodReference.apply(parameter);
        }

        io.opentelemetry.context.Context oldContext = QuarkusContextStorage.INSTANCE.current();
        try (Scope scope = QuarkusContextStorage.INSTANCE.attach(newContext)) {
            return methodReference.apply(parameter);
        } finally {
            if (oldContext != null) {
                QuarkusContextStorage.INSTANCE.attach(oldContext);
            }
        }
    }
}
