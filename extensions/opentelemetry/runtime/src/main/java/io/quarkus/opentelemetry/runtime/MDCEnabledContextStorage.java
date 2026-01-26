package io.quarkus.opentelemetry.runtime;

import org.jboss.logging.Logger;

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
    private static final Logger log = Logger.getLogger(MDCEnabledContextStorage.class);

    @Override
    public Scope attach(Context toAttach) {
        Context beforeAttach = current();

        if (log.isDebugEnabled()) {
            log.debugv("Setting Otel context: {0}", OpenTelemetryUtil.getSpanData(toAttach));
        }
        Scope scope = DEFAULT_CONTEXT_STORAGE.attach(toAttach);
        OpenTelemetryUtil.setMDCData(toAttach, null);

        return new Scope() {
            @Override
            public void close() {
                if (log.isDebugEnabled()) {
                    log.debugv("Closing Otel context: {0}", OpenTelemetryUtil.getSpanData(toAttach));
                }
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
