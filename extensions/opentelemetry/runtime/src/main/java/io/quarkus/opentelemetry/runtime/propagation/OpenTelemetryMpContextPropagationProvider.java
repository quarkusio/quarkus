package io.quarkus.opentelemetry.runtime.propagation;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.logging.Logger;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

public class OpenTelemetryMpContextPropagationProvider implements ThreadContextProvider {

    private static final Logger logger = Logger.getLogger(OpenTelemetryMpContextPropagationProvider.class);

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {

        final Context capturedContext = QuarkusContextStorage.INSTANCE.current();
        // Use anonymous classes instead of lambdas for the native image
        return new ThreadContextSnapshot() {

            @Override
            public ThreadContextController begin() {
                if (capturedContext != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debugv("begin(): attaching captured OTel context: {0}",
                                OpenTelemetryUtil.getSpanData(capturedContext));
                    }
                    Scope scope = QuarkusContextStorage.INSTANCE.attach(capturedContext);
                    if (logger.isDebugEnabled()) {
                        logger.debugv("begin(): obtained scope: {0} (class: {1})",
                                scope, scope.getClass().getName());
                    }
                    return new ThreadContextController() {
                        @Override
                        public void endContext() throws IllegalStateException {
                            Context current = QuarkusContextStorage.INSTANCE.current();
                            if (logger.isDebugEnabled()) {
                                logger.debugv(
                                        "endContext(): current context: {0}, captured context: {1}, scope class: {2}",
                                        current == null ? "null" : OpenTelemetryUtil.getSpanData(current),
                                        OpenTelemetryUtil.getSpanData(capturedContext),
                                        scope.getClass().getName());
                            }
                            // Guard: only close the scope if the current context is still what
                            // begin() attached. If other OTel scopes have closed between begin()
                            // and now (e.g. spans ending during async processing), the DC has
                            // already been cleaned up — unconditionally restoring the stale
                            // otelBeforeAttach would trash it. This mirrors the guard in
                            // ThreadLocalContextStorage.ScopeImpl.close().
                            if (current == capturedContext) {
                                scope.close();
                                if (logger.isDebugEnabled()) {
                                    Context afterClose = QuarkusContextStorage.INSTANCE.current();
                                    logger.debugv("endContext(): context after close: {0}",
                                            afterClose == null ? "null"
                                                    : OpenTelemetryUtil.getSpanData(afterClose));
                                }
                            } else if (logger.isDebugEnabled()) {
                                logger.debugv(
                                        "endContext(): skipping scope.close() — context was changed by other scopes");
                            }
                        }
                    };
                }
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        // nothing to do
                    }
                };
            }

        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        // Use anonymous classes instead of lambdas for the native image
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        // nothing to do
                    }
                };
            }
        };
    }

    @Override
    public String getThreadContextType() {
        return "OpenTelemetry";
    }
}
