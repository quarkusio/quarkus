package io.quarkus.opentelemetry.runtime.propagation;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

public class OpenTelemetryMpContextPropagationProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {

        io.opentelemetry.context.Context context = QuarkusContextStorage.INSTANCE.current();

        // Use anonymous classes instad of lambdas for the native image
        return new ThreadContextSnapshot() {

            @Override
            public ThreadContextController begin() {
                io.opentelemetry.context.Context currentContext = QuarkusContextStorage.INSTANCE.current();
                if (context != null) {
                    QuarkusContextStorage.INSTANCE.attach(context);
                    return new ThreadContextController() {
                        @Override
                        public void endContext() throws IllegalStateException {
                            QuarkusContextStorage.INSTANCE.attach(currentContext);
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
        // Use anonymous classes instad of lambdas for the native image
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        // nothring to do
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
