package io.quarkus.opentelemetry.runtime;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;

/**
 * A Context Storage that wraps the default OpenTelemetry ContextStorage and
 * adds MDC functionality.
 */
enum MDCEnabledContextStorage implements ContextStorage {
    INSTANCE;

    private static final ContextStorage DEFAULT_CONTEXT_STORAGE = ContextStorage.defaultStorage();

    @Override
    public Scope attach(Context toAttach) {
        Context beforeAttach = current();

        OpenTelemetryUtil.setMDCData(toAttach, null);

        Scope scope = DEFAULT_CONTEXT_STORAGE.attach(toAttach);

        return new Scope() {
            @Override
            public void close() {
                if (beforeAttach == null) {
                    OpenTelemetryUtil.clearMDCData(null);
                } else {
                    OpenTelemetryUtil.setMDCData(beforeAttach, null);
                }
                scope.close();
            }
        };
    }

    @Override
    public Context current() {
        return DEFAULT_CONTEXT_STORAGE.current();
    }
}
