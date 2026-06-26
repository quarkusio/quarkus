package io.quarkus.observation.propagation;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.micrometer.observation.Observation;

public class ObservationMpContextPropagationProvider implements ThreadContextProvider {

    private static final ThreadContextController NOOP_CONTROLLER = new ThreadContextController() {
        @Override
        public void endContext() throws IllegalStateException {
        }
    };

    private static final ThreadContextSnapshot NOOP_SNAPSHOT = new ThreadContextSnapshot() {
        @Override
        public ThreadContextController begin() {
            return NOOP_CONTROLLER;
        }
    };

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        Observation.Scope capturedScope = ObservationContextStorage.currentScope();

        // Use anonymous classes instead of lambdas for the native image
        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                if (capturedScope == null) {
                    return NOOP_CONTROLLER;
                }

                Observation.Scope previousScope = ObservationContextStorage.setCurrentScope(capturedScope);

                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        ObservationContextStorage.setCurrentScope(previousScope);
                    }
                };
            }
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return NOOP_SNAPSHOT;
    }

    @Override
    public String getThreadContextType() {
        return "MicrometerObservation";
    }
}
