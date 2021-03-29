package io.quarkus.opentelemetry;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class OpenTelemetryContextProvider implements ThreadContextProvider {
    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> map) {
        Context capturedContext = Context.current();
        return () -> {
            Context context = Context.current();
            if (capturedContext != context) {
                capturedContext.makeCurrent();
            }
            return () -> {
                if (Context.current() != context) {
                    context.makeCurrent();
                }
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> map) {
        return () -> {
            Scope emptyScope = Context.root().makeCurrent();
            return emptyScope::close;
        };
    }

    @Override
    public String getThreadContextType() {
        return "OpenTelemetry";
    }
}
